package com.weatherxm.ui.rewarddetails

import android.os.Bundle
import android.view.View
import com.weatherxm.R
import com.weatherxm.data.BoostReward
import com.weatherxm.data.Reward
import com.weatherxm.data.RewardDetails
import com.weatherxm.data.RewardsAnnotationGroup
import com.weatherxm.data.SeverityLevel
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityRewardDetailsBinding
import com.weatherxm.ui.common.AnnotationGroupCode
import com.weatherxm.ui.common.AnnotationGroupCode.LOCATION_NOT_VERIFIED
import com.weatherxm.ui.common.AnnotationGroupCode.NO_LOCATION_DATA
import com.weatherxm.ui.common.AnnotationGroupCode.USER_RELOCATION_PENALTY
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.Contracts.ARG_REWARD
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.util.Analytics
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.Rewards.formatTokens
import com.weatherxm.util.Rewards.isPoL
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.time.ZonedDateTime

class RewardDetailsActivity : BaseActivity(), RewardBoostListener {
    private lateinit var binding: ActivityRewardDetailsBinding
    private val model: RewardDetailsViewModel by viewModel {
        parametersOf(intent.parcelable<UIDevice>(ARG_DEVICE))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        val reward = intent?.extras?.parcelable<Reward>(ARG_REWARD)
        if (model.device.isEmpty() || reward == null || reward.timestamp == null) {
            Timber.d("Could not start RewardDetailsActivity. Device or Rewards object is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        with(binding.toolbar) {
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
        val formattedDate = reward.timestamp.getFormattedDate(true)
        val subtitle = "${getString(R.string.earnings_for, formattedDate)} (UTC)"
        binding.header
            .subtitle(subtitle)
            .infoButton {
                navigator.showMessageDialog(
                    supportFragmentManager,
                    getString(R.string.daily_reward),
                    getString(R.string.daily_reward_explanation),
                    readMoreUrl = getString(R.string.docs_url_reward_mechanism)
                )
            }

        model.onRewardDetails().observe(this) {
            when (it.status) {
                Status.SUCCESS -> {
                    it.data?.let { rewardDetails ->
                        updateUI(rewardDetails)
                    } ?: onFetchError(
                        reward.timestamp, formattedDate, getString(R.string.error_reach_out)
                    )
                }
                Status.ERROR -> {
                    onFetchError(reward.timestamp, formattedDate, it.message)
                }
                Status.LOADING -> {
                    binding.statusView.clear().animation(R.raw.anim_loading)
                    binding.mainContainer.setVisible(false)
                    binding.statusView.setVisible(true)
                }
            }
        }

        model.fetchRewardDetails(reward.timestamp)
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(Analytics.Screen.DEVICE_REWARD_DETAILS, this::class.simpleName)
    }

    private fun updateUI(data: RewardDetails) {
        val actualBaseReward = formatTokens(data.base?.actualReward)
        val maxBaseReward = formatTokens(data.base?.maxReward)
        val sortedIssues = data.toSortedAnnotations()
        val boostAdapter = RewardBoostAdapter(this)

        binding.totalDailyReward.text =
            getString(R.string.reward, formatTokens(data.totalDailyReward))
        binding.baseRewardDesc.setHtml(
            getString(R.string.base_reward_desc, actualBaseReward, maxBaseReward)
        )

        updateIssues(sortedIssues)

        data.base?.qodScore?.let {
            updateDataQualityCard(it)
        } ?: binding.dataQualityCard.desc(getString(R.string.data_quality_not_available))
        updateLocationCard(sortedIssues)
        updateCellCard(sortedIssues)

        binding.boostsRecycler.adapter = boostAdapter
        boostAdapter.submitList(data.boost?.data)
        binding.boostsDesc.setHtml(
            getString(R.string.active_boosts_desc, formatTokens(data.boost?.totalDailyReward))
        )
        val hasBoosts = data.boost?.data == null || data.boost.data.isEmpty()
        binding.boostsRecycler.setVisible(!hasBoosts)
        binding.boostsDesc.setVisible(!hasBoosts)
        binding.noActiveBoostsCard.setVisible(hasBoosts)

        binding.statusView.setVisible(false)
        binding.mainContainer.setVisible(true)
    }

    private fun updateIssues(sortedIssues: List<RewardsAnnotationGroup>?) {
        if (sortedIssues.isNullOrEmpty()) {
            binding.issuesTitle.setVisible(false)
            binding.issuesDesc.setVisible(false)
            binding.issueCard.setVisible(false)
        } else {
            val issuesSize = sortedIssues.size
            val topIssue = sortedIssues[0]
            binding.issuesDesc.setHtml(
                if (topIssue.isInfo() && issuesSize > 1) {
                    getString(R.string.annotation_issues_info_text, issuesSize)
                } else if (topIssue.isInfo() && issuesSize <= 1) {
                    getString(R.string.annotation_issue_info_text)
                } else if (issuesSize > 1) {
                    getString(R.string.annotation_issues_warn_error_text, issuesSize)
                } else {
                    getString(R.string.annotation_issue_warn_error_text)
                }
            )

            onIssueCard(topIssue, issuesSize > 1)
        }
    }

    @Suppress("MagicNumber", "CyclomaticComplexMethod")
    private fun updateDataQualityCard(qodScore: Int) {
        binding.dataQualityCard.infoButton {
            navigator.showMessageDialog(
                supportFragmentManager,
                getString(R.string.data_quality),
                getString(R.string.data_quality_explanation),
                readMoreUrl = getString(R.string.docs_url_qod_algorithm)
            )
        }
        if (qodScore >= 95) {
            binding.dataQualityCard.checkmark()
        } else if (qodScore in 10..94) {
            binding.dataQualityCard.warning()
        } else {
            binding.dataQualityCard.error()
        }
        binding.dataQualityCard.setIconColor(qodScore)

        val qodMessageResId = when {
            qodScore == 100 -> R.string.data_quality_perfect
            qodScore >= 95 -> R.string.data_quality_almost_perfect
            qodScore >= 80 -> {
                if (model.device.isOwned()) R.string.data_quality_great
                else R.string.data_quality_great_public
            }
            qodScore >= 60 -> {
                if (model.device.isOwned()) R.string.data_quality_ok
                else R.string.data_quality_ok_public
            }
            qodScore >= 40 -> {
                if (model.device.isOwned()) R.string.data_quality_average
                else R.string.data_quality_average_public
            }
            qodScore >= 20 -> {
                if (model.device.isOwned()) R.string.data_quality_low
                else R.string.data_quality_low_public
            }
            qodScore > 0 -> {
                if (model.device.isOwned()) R.string.data_quality_very_low
                else R.string.data_quality_very_low_public
            }
            qodScore == 0 -> R.string.data_quality_no_rewards
            else -> R.string.data_quality_not_available
        }

        if (qodScore in 1..99) {
            binding.dataQualityCard.desc(getString(qodMessageResId, qodScore))
        } else {
            binding.dataQualityCard.desc(getString(qodMessageResId))
        }
        binding.dataQualityCard.setSlider(qodScore)
    }

    private fun updateLocationCard(annotations: List<RewardsAnnotationGroup>?) {
        binding.locationQualityCard.infoButton {
            navigator.showMessageDialog(
                supportFragmentManager,
                getString(R.string.location_quality),
                getString(R.string.location_quality_explanation),
                readMoreUrl = getString(R.string.docs_url_pol_algorithm)
            )
        }
        annotations?.firstOrNull {
            it.toAnnotationGroupCode().isPoL()
        }?.let {
            when (it.severityLevel) {
                SeverityLevel.WARNING -> binding.locationQualityCard.warning()
                SeverityLevel.ERROR -> binding.locationQualityCard.error()
                else -> binding.locationQualityCard.checkmark()
            }
            binding.locationQualityCard.desc(
                when (it.toAnnotationGroupCode()) {
                    NO_LOCATION_DATA -> getString(R.string.location_quality_no_location)
                    LOCATION_NOT_VERIFIED -> getString(R.string.location_quality_not_verified)
                    USER_RELOCATION_PENALTY -> getString(R.string.location_quality_relocated)
                    else -> {
                        Timber.e(
                            IllegalStateException("Wrong PoL code: ${it.toAnnotationGroupCode()}")
                        )
                        String.empty()
                    }
                }
            )
        } ?: run {
            binding.locationQualityCard.desc(getString(R.string.location_quality_verified))
            binding.locationQualityCard.checkmark()
        }
    }

    private fun updateCellCard(annotations: List<RewardsAnnotationGroup>?) {
        binding.cellQualityCard.infoButton {
            navigator.showMessageDialog(
                supportFragmentManager,
                getString(R.string.cell_position),
                getString(R.string.cell_position_explanation),
                readMoreUrl = getString(R.string.docs_url_cell_capacity)
            )
        }
        annotations?.firstOrNull {
            it.toAnnotationGroupCode() == AnnotationGroupCode.CELL_CAPACITY_REACHED
        }?.let {
            when (it.severityLevel) {
                SeverityLevel.WARNING -> binding.cellQualityCard.warning()
                SeverityLevel.ERROR -> binding.cellQualityCard.error()
                else -> binding.cellQualityCard.checkmark()
            }
            binding.cellQualityCard.desc(it.message ?: String.empty())
        } ?: run {
            binding.cellQualityCard.desc(getString(R.string.cell_position_great))
            binding.cellQualityCard.checkmark()
        }
    }

    private fun onFetchError(timestamp: ZonedDateTime, formattedDate: String, subtitle: String?) {
        binding.statusView.clear()
            .animation(R.raw.anim_error)
            .title(getString(R.string.error_reward_details_title, formattedDate))
            .subtitle(subtitle)
            .action(getString(R.string.action_retry))
            .listener {
                model.fetchRewardDetails(timestamp)
            }
        binding.mainContainer.setVisible(false)
        binding.statusView.visibility = View.VISIBLE
    }

    private fun onIssueCard(
        issue: RewardsAnnotationGroup,
        hasMoreThanOneIssues: Boolean
    ) {
        val strokeColor: Int
        val backgroundColor: Int
        when (issue.severityLevel) {
            SeverityLevel.INFO -> {
                strokeColor = R.color.light_lightest_blue
                backgroundColor = R.color.blueTint
            }
            SeverityLevel.WARNING -> {
                strokeColor = R.color.warning
                backgroundColor = R.color.warningTint
            }
            SeverityLevel.ERROR -> {
                strokeColor = R.color.error
                backgroundColor = R.color.errorTint
            }
            else -> {
                strokeColor = R.color.light_lightest_blue
                backgroundColor = R.color.blueTint
            }
        }

        binding.issueCard
            .setStrokeColor(strokeColor)
            .setBackground(backgroundColor)
            .title(issue.title)
            .message(issue.message)

        if (hasMoreThanOneIssues) {
            binding.issueCard.action(getString(R.string.action_view_all_issues)) {
                navigator.showRewardIssues(this, model.device, model.onRewardDetails().value?.data)
            }
        } else {
            val code = issue.toAnnotationGroupCode()
            if (code == AnnotationGroupCode.NO_WALLET && model.device.isOwned()) {
                binding.issueCard.action(getString(R.string.add_wallet_now)) {
                    navigator.showConnectWallet(this)
                }
            } else if (code == LOCATION_NOT_VERIFIED && model.device.isOwned()) {
                binding.issueCard.action(getString(R.string.edit_location)) {
                    navigator.showEditLocation(null, this, model.device)
                }
            } else if (!issue.docUrl.isNullOrEmpty()) {
                binding.issueCard.action(getString(R.string.read_more)) {
                    navigator.openWebsite(this, issue.docUrl)
                }
            }
        }
        binding.issueCard.setVisible(true)
    }

    override fun onBoostReward(boost: BoostReward) {
        // TODO: Open Boost Reward Details screen
    }
}
