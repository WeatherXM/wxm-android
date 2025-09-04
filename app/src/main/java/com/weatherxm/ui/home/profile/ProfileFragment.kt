package com.weatherxm.ui.home.profile

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.ui.unit.dp
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.models.SeverityLevel
import com.weatherxm.data.models.User
import com.weatherxm.databinding.FragmentProfileBinding
import com.weatherxm.ui.common.ActionForMessageView
import com.weatherxm.ui.common.Contracts.ARG_TOKEN_CLAIMED_AMOUNT
import com.weatherxm.ui.common.Contracts.NOT_AVAILABLE_VALUE
import com.weatherxm.ui.common.DataForMessageView
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.SubtitleForMessageView
import com.weatherxm.ui.common.UIWalletRewards
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.setCardStroke
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.components.ProPromotionDialogFragment
import com.weatherxm.ui.components.compose.MessageCardView
import com.weatherxm.ui.components.compose.ProPromotionCard
import com.weatherxm.ui.home.HomeActivity
import com.weatherxm.ui.home.HomeViewModel
import com.weatherxm.util.Mask
import com.weatherxm.util.NumberUtils.formatTokens
import com.weatherxm.util.NumberUtils.toBigDecimalSafe
import com.weatherxm.util.NumberUtils.weiToETH
import dev.chrisbanes.insetter.applyInsetter
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber

class ProfileFragment : BaseFragment() {
    private lateinit var binding: FragmentProfileBinding
    private val model: ProfileViewModel by activityViewModel()
    private val parentModel: HomeViewModel by activityViewModel()

    // Register the launcher for the connect wallet activity and wait for a possible result
    private val connectWalletLauncher =
        registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                parentModel.setWalletNotMissing()
                model.fetchUser(true)
            }
        }

    // Register the launcher for the rewards claim activity and wait for a possible result
    private val rewardsClaimLauncher =
        registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            trackClaimingResult(result.resultCode == Activity.RESULT_OK)
            if (result.resultCode == Activity.RESULT_OK) {
                model.onClaimedResult(
                    result.data?.getDoubleExtra(ARG_TOKEN_CLAIMED_AMOUNT, 0.0) ?: 0.0
                )
            } else {
                showSnackbarMessage(
                    binding.root,
                    getString(R.string.having_trouble_claiming),
                    callback = { snackbar?.dismiss() },
                    R.string.action_close,
                    (activity as HomeActivity).navView()
                )
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        binding.root.applyInsetter {
            type(statusBars = true) {
                padding(left = false, top = true, right = false, bottom = false)
            }
        }

        binding.swiperefresh.setOnRefreshListener {
            model.fetchUser(parentModel.isLoggedIn())
            if (parentModel.isLoggedIn()) {
                parentModel.getSurvey()
            }
        }

        binding.walletContainerCard.setOnClickListener {
            navigator.showConnectWallet(connectWalletLauncher, this)
        }

        binding.settingsContainerCard.setOnClickListener {
            navigator.showPreferences(this)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.totalEarnedInfoBtn.setOnClickListener {
            navigator.showMessageDialog(
                childFragmentManager,
                title = getString(R.string.total_earned),
                htmlMessage = getString(R.string.total_earned_desc)
            )
        }

        binding.totalClaimedInfoBtn.setOnClickListener {
            navigator.showMessageDialog(
                childFragmentManager,
                title = getString(R.string.total_claimed),
                message = getString(R.string.total_claimed_desc)
            )
        }

        model.onUser().observe(viewLifecycleOwner) { resource ->
            Timber.d("Data updated: ${resource.status}")
            when (resource.status) {
                Status.SUCCESS -> {
                    updateUserUI(resource.data)
                    toggleLoading(false)
                }
                Status.ERROR -> {
                    Timber.d("Got error: $resource.message")
                    resource.message?.let { context.toast(it) }
                    onNotAvailableRewards()
                    toggleLoading(false)
                }
                Status.LOADING -> {
                    toggleLoading(true)
                }
            }
        }

        model.onWalletRewards().observe(viewLifecycleOwner) { resource ->
            Timber.d("Data updated: ${resource.status}")
            when (resource.status) {
                Status.SUCCESS -> {
                    resource.data?.let {
                        updateRewardsUI(it)
                    }
                    toggleLoading(false)
                }
                Status.ERROR -> {
                    Timber.d("Got error: $resource.message")
                    onNotAvailableRewards()
                    resource.message?.let { context.toast(it) }
                    toggleLoading(false)
                }
                Status.LOADING -> {
                    toggleLoading(true)
                }
            }
        }

        parentModel.onSurvey().observe(viewLifecycleOwner) {
            binding.surveyCard
                .title(it.title)
                .message(it.message)
                .action(it.actionLabel) {
                    navigator.openWebsite(context, it.url)
                }
                .close {
                    parentModel.dismissSurvey(it.id)
                    binding.surveyCard.visible(false)
                }
                .visible(true)
        }

        model.onLoggedOutUser().observe(viewLifecycleOwner) {
            onLoggedOutUser()
        }

        initProPromotionCard()

        model.fetchUser(parentModel.isLoggedIn())
        if (parentModel.isLoggedIn()) {
            parentModel.getSurvey()
        }
    }

    private fun initProPromotionCard() {
        binding.proPromotionCard.setContent {
            ProPromotionCard(R.string.take_weather_insights_next_level) {
                analytics.trackEventSelectContent(
                    AnalyticsService.ParamValue.PRO_PROMOTION_CTA.paramValue,
                    Pair(
                        FirebaseAnalytics.Param.SOURCE,
                        AnalyticsService.ParamValue.LOCAL_PROFILE.paramValue
                    )
                )
                ProPromotionDialogFragment().show(this)
            }
        }
    }

    private fun toggleLoading(isLoading: Boolean) {
        if (isLoading) {
            if (!binding.swiperefresh.isRefreshing) {
                binding.progress.visible(true)
            } else {
                binding.swiperefresh.isRefreshing = false
                binding.progress.invisible()
            }
        } else {
            binding.swiperefresh.isRefreshing = false
            binding.progress.invisible()
        }
    }

    private fun onLoggedOutUser() {
        binding.toolbar.subtitle = getString(R.string.login_to_see_rewards)
        binding.totalsRewardsContainer.visible(false)
        binding.rewardsContainerCard.visible(false)
        binding.walletContainerCard.visible(false)
        binding.proPromotionCard.visible(false)
        binding.progress.invisible()
        binding.swiperefresh.isRefreshing = false
    }

    private fun onNotAvailableRewards() {
        binding.totalEarnedValue.text = NOT_AVAILABLE_VALUE
        binding.totalClaimedValue.text = NOT_AVAILABLE_VALUE
        binding.rewards.subtitle(NOT_AVAILABLE_VALUE)
    }

    private fun updateRewardsUI(data: UIWalletRewards) {
        binding.rewards.clear()
        binding.totalEarnedValue.text =
            getString(
                R.string.wxm_amount,
                formatTokens(weiToETH(data.totalEarned.toBigDecimalSafe()))
            )
        binding.totalClaimedValue.text =
            getString(
                R.string.wxm_amount,
                formatTokens(weiToETH(data.totalClaimed.toBigDecimalSafe()))
            )
        if (data.allocated == 0.0) {
            binding.rewards.subtitle(getString(R.string.no_allocated_rewards))

            if (parentModel.hasDevices() == false) {
                binding.rewardsContainerCard.setCardStroke(R.color.colorPrimary, 2)
                binding.allocatedRewardsSecondaryCard.setContent {
                    MessageCardView(
                        data = DataForMessageView(
                            extraTopPadding = 24.dp,
                            title = R.string.start_earning,
                            subtitle = SubtitleForMessageView(
                                message = R.string.start_earning_desc
                            ),
                            action = ActionForMessageView(
                                label = R.string.action_buy_a_station,
                                backgroundTint = R.color.colorPrimary,
                                foregroundTint = R.color.colorOnPrimary,
                                startIcon = R.drawable.ic_cart,
                                onClickListener = {
                                    navigator.openWebsite(
                                        requireContext(),
                                        getString(R.string.shop_url)
                                    )
                                }
                            )
                        ))
                }
                binding.allocatedRewardsSecondaryCard.visible(true)
            }
        } else {
            binding.rewardsContainerCard.strokeWidth = 0
            binding.allocatedRewardsSecondaryCard.setContent {
                MessageCardView(
                    data = DataForMessageView(
                        extraTopPadding = 24.dp,
                        subtitle = SubtitleForMessageView(
                            htmlMessage = R.string.allocated_rewards_alternative_claiming
                        )
                    )
                )
            }
            binding.allocatedRewardsSecondaryCard.visible(true)
            binding.rewards
                .subtitle(
                    getString(
                        R.string.wxm_amount,
                        formatTokens(weiToETH(data.allocated.toBigDecimalSafe()))
                    )
                )
                .action(getString(R.string.action_claim)) {
                    navigator.showRewardsClaiming(rewardsClaimLauncher, requireContext(), data)
                }
        }
        binding.totalsRewardsContainer.visible(true)
        binding.rewardsContainerCard.visible(true)
    }

    private fun updateUserUI(user: User?) {
        binding.wallet.clear()
        binding.toolbar.subtitle = user?.email
        user?.wallet?.address?.let {
            binding.noWalletCard.visible(false)
            binding.walletContainerCard.strokeWidth = 0
            binding.wallet.chipSubtitle(Mask.maskHash(it))
        } ?: kotlin.run {
            binding.wallet.subtitle(getString(R.string.no_wallet_added))
            if (parentModel.hasDevices() == true) {
                binding.noWalletCard.setContent {
                    MessageCardView(
                        data = DataForMessageView(
                            extraTopPadding = 24.dp,
                            title = R.string.losing_rewards,
                            subtitle = SubtitleForMessageView(
                                message = R.string.losing_rewards_desc
                            ),
                            drawable = R.drawable.ic_error_hex_filled,
                            severityLevel = SeverityLevel.ERROR,
                        )
                    )
                }
                binding.noWalletCard.visible(true)
                binding.walletContainerCard.setCardStroke(R.color.error, 2)
            }
        }
        binding.walletContainerCard.visible(true)
        binding.proPromotionCard.visible(true)
    }

    private fun trackClaimingResult(isSuccess: Boolean) {
        val statusValue = if (isSuccess) {
            AnalyticsService.ParamValue.SUCCESS_ID.paramValue
        } else {
            AnalyticsService.ParamValue.FAILURE_ID.paramValue
        }
        analytics.trackEventViewContent(
            contentName = AnalyticsService.ParamValue.TOKEN_CLAIMING_RESULT.paramValue,
            customParams = arrayOf(Pair(AnalyticsService.CustomParam.STATE.paramName, statusValue))
        )
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.PROFILE, classSimpleName())
    }
}
