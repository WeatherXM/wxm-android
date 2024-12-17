package com.weatherxm.ui.rewarddetails

import android.os.Bundle
import android.widget.Toast
import coil3.ImageLoader
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.models.BoostCode
import com.weatherxm.data.models.BoostReward
import com.weatherxm.data.models.Reward
import com.weatherxm.data.models.RewardDetails
import com.weatherxm.data.models.RewardsAnnotationGroup
import com.weatherxm.data.models.SeverityLevel
import com.weatherxm.databinding.ActivityRewardDetailsBinding
import com.weatherxm.ui.common.AnnotationGroupCode
import com.weatherxm.ui.common.AnnotationGroupCode.LOCATION_NOT_VERIFIED
import com.weatherxm.ui.common.AnnotationGroupCode.NO_LOCATION_DATA
import com.weatherxm.ui.common.AnnotationGroupCode.USER_RELOCATION_PENALTY
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.Contracts.ARG_REWARD
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.NumberUtils.formatTokens
import com.weatherxm.util.Rewards.isPoL
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.time.ZonedDateTime

class RewardDetailsActivity : BaseActivity(), RewardBoostListener {
    private lateinit var binding: ActivityRewardDetailsBinding
    private val model: RewardDetailsViewModel by viewModel {
        parametersOf(intent.parcelable<UIDevice>(ARG_DEVICE))
    }
    private val imageLoader: ImageLoader by inject()

    private var rewardDate: String = String.empty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        rewardDate = reward.timestamp.getFormattedDate(true)
        val subtitle = "${getString(R.string.earnings_for, rewardDate)} (UTC)"
        binding.header
            .subtitle(subtitle)
            .infoButton {
                onMessageDialog(
                    AnalyticsService.ParamValue.INFO_DAILY_REWARDS.paramValue,
                    getString(R.string.daily_reward),
                    getString(R.string.daily_reward_explanation),
                    readMoreUrl = getString(R.string.docs_url_reward_mechanism),
                    analyticsScreen = AnalyticsService.Screen.DAILY_REWARD_INFO
                )
            }

        model.onRewardDetails().observe(this) {
            when (it.status) {
                Status.SUCCESS -> {
                    it.data?.let { rewardDetails ->
                        updateUI(rewardDetails)
                    } ?: onFetchError(
                        reward.timestamp, rewardDate, getString(R.string.error_reach_out)
                    )
                }
                Status.ERROR -> {
                    onFetchError(reward.timestamp, rewardDate, it.message)
                }
                Status.LOADING -> {
                    binding.statusView.clear().animation(R.raw.anim_loading)
                    binding.mainContainer.visible(false)
                    binding.statusView.visible(true)
                }
            }
        }

        model.fetchRewardDetails(reward.timestamp)
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.DEVICE_REWARD_DETAILS, classSimpleName())
    }

    private fun updateUI(data: RewardDetails) {
        val actualBaseReward = formatTokens(data.base?.actualReward)
        val maxBaseReward = formatTokens(data.base?.maxReward)
        val sortedIssues = data.toSortedAnnotations()
        val boostAdapter = RewardBoostAdapter(imageLoader, this)

        binding.totalDailyReward.text =
            getString(R.string.reward, formatTokens(data.totalDailyReward))
        binding.baseRewardDesc.setHtml(
            getString(R.string.base_reward_desc, actualBaseReward, maxBaseReward)
        )

        handleSplitRewards(data)

        updateIssues(sortedIssues)

        data.base?.qodScore?.let {
            updateDataQualityCard(it)
        } ?: run {
            binding.dataQualityCard.hideIcon().desc(getString(R.string.data_quality_not_available))
        }
        updateLocationCard(sortedIssues)
        updateCellCard(sortedIssues)

        binding.boostsRecycler.adapter = boostAdapter
        boostAdapter.submitList(data.boost?.data)
        binding.boostsDesc.setHtml(
            getString(R.string.active_boosts_desc, formatTokens(data.boost?.totalDailyReward))
        )
        val hasBoosts = data.boost?.data == null || data.boost.data.isEmpty()
        binding.boostsRecycler.visible(!hasBoosts)
        binding.boostsDesc.visible(!hasBoosts)
        binding.noActiveBoostsCard.visible(hasBoosts)

        binding.statusView.visible(false)
        binding.mainContainer.visible(true)
    }

    private fun handleSplitRewards(data: RewardDetails) {
        if (!data.hasSplitRewards()) {
            val stakeHolderValue = if (model.device.isOwned()) {
                AnalyticsService.ParamValue.STAKEHOLDER_LOWERCASE.paramValue
            } else {
                AnalyticsService.ParamValue.NON_STAKEHOLDER.paramValue
            }
            trackRewardSplitViewContent(
                AnalyticsService.ParamValue.NO_REWARD_SPLITTING.paramValue,
                stakeHolderValue
            )
        } else {
            model.getRewardSplitsData {
                val stakeHolderValue = if (model.isStakeHolder()) {
                    AnalyticsService.ParamValue.STAKEHOLDER_LOWERCASE.paramValue
                } else {
                    AnalyticsService.ParamValue.NON_STAKEHOLDER.paramValue
                }
                trackRewardSplitViewContent(
                    AnalyticsService.ParamValue.REWARD_SPLITTING.paramValue,
                    stakeHolderValue
                )
            }
            binding.showSplitBtn.setOnClickListener {
                analytics.trackEventUserAction(
                    AnalyticsService.ParamValue.REWARD_SPLIT_PRESSED.paramValue,
                    AnalyticsService.ParamValue.STAKEHOLDER.paramValue,
                    Pair(
                        AnalyticsService.CustomParam.STATE.paramName,
                        model.isStakeHolder().toString().lowercase()
                    )
                )
                RewardSplitDialogFragment().show(this)
            }
            binding.showSplitBtn.visible(true)
        }
    }

    private fun trackRewardSplitViewContent(deviceState: String, userState: String) {
        analytics.trackEventViewContent(
            AnalyticsService.ParamValue.REWARD_SPLITTING_DAILY_REWARD.paramValue,
            contentId = null,
            Pair(AnalyticsService.CustomParam.DEVICE_STATE.paramName, deviceState),
            Pair(AnalyticsService.CustomParam.USER_STATE.paramName, userState)
        )
    }

    private fun updateIssues(sortedIssues: List<RewardsAnnotationGroup>?) {
        if (sortedIssues.isNullOrEmpty()) {
            binding.issuesTitle.visible(false)
            binding.issuesDesc.visible(false)
            binding.issueCard.visible(false)
        } else {
            val issuesSize = sortedIssues.size
            binding.issuesDesc.setHtml(
                if (sortedIssues[0].isInfo()) {
                    resources.getQuantityString(
                        R.plurals.annotation_issue_info_text, issuesSize, issuesSize
                    )
                } else {
                    resources.getQuantityString(
                        R.plurals.annotation_issue_warn_error_text, issuesSize, issuesSize
                    )
                }
            )

            onIssueCard(sortedIssues[0], issuesSize > 1)
        }
    }

    @Suppress("MagicNumber", "CyclomaticComplexMethod")
    private fun updateDataQualityCard(qodScore: Int) {
        binding.dataQualityCard.infoButton {
            onMessageDialog(
                AnalyticsService.ParamValue.INFO_QOD.paramValue,
                getString(R.string.data_quality),
                getString(R.string.data_quality_explanation),
                readMoreUrl = getString(R.string.docs_url_qod_algorithm),
                analyticsScreen = AnalyticsService.Screen.DATA_QUALITY_INFO
            )
        }
        if (qodScore >= 80) {
            binding.dataQualityCard.checkmark()
        } else if (qodScore >= 20) {
            binding.dataQualityCard.warning()
        } else {
            binding.dataQualityCard.error()
        }

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
            onMessageDialog(
                AnalyticsService.ParamValue.INFO_POL.paramValue,
                getString(R.string.location_quality),
                getString(R.string.location_quality_explanation),
                readMoreUrl = getString(R.string.docs_url_pol_algorithm),
                analyticsScreen = AnalyticsService.Screen.LOCATION_QUALITY_INFO
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
            onMessageDialog(
                AnalyticsService.ParamValue.INFO_CELL_POSITION.paramValue,
                getString(R.string.cell_ranking),
                getString(R.string.cell_ranking_explanation),
                readMoreUrl = getString(R.string.docs_url_cell_capacity),
                analyticsScreen = AnalyticsService.Screen.CELL_RANKING_INFO
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
            binding.cellQualityCard.desc(getString(R.string.cell_ranking_great))
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
        binding.mainContainer.visible(false)
        binding.statusView.visible(true)
    }

    private fun onIssueCard(
        issue: RewardsAnnotationGroup,
        hasMoreThanOneIssues: Boolean
    ) {
        val strokeColor: Int
        val backgroundColor: Int
        when (issue.severityLevel) {
            SeverityLevel.INFO -> {
                strokeColor = R.color.infoStrokeColor
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
                strokeColor = R.color.infoStrokeColor
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
                    analytics.trackEventSelectContent(
                        AnalyticsService.ParamValue.WEB_DOCUMENTATION.paramValue,
                        Pair(FirebaseAnalytics.Param.ITEM_ID, issue.docUrl)
                    )
                    navigator.openWebsite(this, issue.docUrl)
                }
            }
        }
        binding.issueCard.visible(true)
    }

    private fun onMessageDialog(
        itemId: String,
        title: String,
        message: String,
        readMoreUrl: String,
        analyticsScreen: AnalyticsService.Screen
    ) {
        analytics.trackEventSelectContent(
            AnalyticsService.ParamValue.LEARN_MORE.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_ID, itemId)
        )
        navigator.showMessageDialog(
            supportFragmentManager,
            title = title,
            message = message,
            readMoreUrl = readMoreUrl,
            analyticsScreen = analyticsScreen
        )
    }

    override fun onBoostReward(boost: BoostReward) {
        val isCodeSupported = try {
            BoostCode.valueOf(boost.code ?: String.empty())
            true
        } catch (e: IllegalArgumentException) {
            Timber.e("Unsupported Boost Code: ${boost.code}")
            false
        }
        if (isCodeSupported) {
            navigator.showRewardBoost(this, boost, model.device.id, rewardDate)
        } else {
            toast(R.string.error_boost_not_supported, duration = Toast.LENGTH_LONG)
        }
    }
}
