package com.weatherxm.ui.managesubscription

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.models.SubscriptionOffer
import com.weatherxm.databinding.ActivityManageSubscriptionBinding
import com.weatherxm.service.BillingService
import com.weatherxm.ui.common.Contracts.ARG_HAS_FREE_TRIAL_AVAILABLE
import com.weatherxm.ui.common.Contracts.ARG_IS_LOGGED_IN
import com.weatherxm.ui.common.PurchaseUpdateState
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.components.compose.DowngradeDialog
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ManageSubscriptionActivity : BaseActivity() {
    private lateinit var binding: ActivityManageSubscriptionBinding
    private val model: ManageSubscriptionViewModel by viewModel()
    private val billingService: BillingService by inject()

    private var hasFreeTrialAvailable = false
    private var isLoggedIn = false

    //   private var currentSelectedTab = 1
    private val hasActiveRenewingSub = mutableStateOf(false)
    private val planSelected = mutableStateOf<SubscriptionOffer?>(null)
    private val shouldShowDowngradeDialog = mutableStateOf(false)

    init {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    billingService.getPurchaseUpdates().collect { state ->
                        state?.let {
                            onPurchaseUpdate(it)
                        }
                    }
                }

                launch {
                    billingService.getActiveSubFlow().collect {
                        val availableSub =
                            billingService.getMonthlyAvailableSub(hasFreeTrialAvailable)
//                        val availableSub = if (currentSelectedTab == 0) {
//                            billingService.getMonthlyAvailableSub(hasFreeTrialAvailable)
//                        } else {
//                            billingService.getAnnualAvailableSub(hasFreeTrialAvailable)
//                        }
                        if (it == null || !it.isAutoRenewing) {
                            hasActiveRenewingSub.value = false
                            binding.toolbar.title = getString(R.string.upgrade_to_premium)
                            binding.planComposable.setContent {
                                Column(
                                    modifier = Modifier.padding(
                                        bottom = dimensionResource(R.dimen.margin_normal)
                                    )
                                ) {
                                    FreePlanView(
                                        isSelected = planSelected.value == null,
                                        isCurrentPlan = !hasActiveRenewingSub.value
                                    ) {
                                        onFreeSelected()
                                    }
                                    PremiumPlanView(
                                        sub = availableSub,
                                        isSelected = planSelected.value == availableSub,
                                        isCurrentPlan = hasActiveRenewingSub.value,
                                        hasFreeTrialAvailable = hasFreeTrialAvailable
                                    ) {
                                        onPremiumSelected(availableSub)
                                    }
                                }
                            }
                            binding.cancelAnytimeText.visible(true)
                            //    binding.subscriptionTabSelector.visible(true)
                        } else {
                            hasActiveRenewingSub.value = true
                            binding.toolbar.title = getString(R.string.manage_subscription)
                            binding.planComposable.setContent {
                                Column(
                                    modifier = Modifier.padding(
                                        bottom = dimensionResource(R.dimen.margin_normal)
                                    )
                                ) {
                                    PremiumPlanView(
                                        sub = availableSub,
                                        isSelected = planSelected.value == availableSub,
                                        isCurrentPlan = hasActiveRenewingSub.value,
                                        hasFreeTrialAvailable = false
                                    ) {
                                        onPremiumSelected(availableSub)
                                    }
                                    FreePlanView(
                                        isSelected = planSelected.value == null,
                                        isCurrentPlan = !hasActiveRenewingSub.value
                                    ) {
                                        onFreeSelected()
                                    }
                                }
                            }
                            binding.cancelAnytimeText.visible(false)
                            //    binding.subscriptionTabSelector.visible(false)
                        }
                        planSelected.value = availableSub
                        initActionButtonForPremiumSelected()
                        binding.toolbar.subtitle =
                            getString(R.string.get_the_most_accurate_forecasts)
                        binding.mainActionBtn.visible(true)
                    }
                }

                launch {
                    billingService.setupPurchases(false)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageSubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hasFreeTrialAvailable = intent?.extras?.getBoolean(ARG_HAS_FREE_TRIAL_AVAILABLE) == true
        isLoggedIn = intent?.extras?.getBoolean(ARG_IS_LOGGED_IN) == true

        with(binding.toolbar) {
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }

//        binding.subscriptionTabSelector.setContent {
//            SubscriptionTabSelector(1) { newSelectedTab ->
//                currentSelectedTab = newSelectedTab
//                if (newSelectedTab == 0) {
//                    billingService.getMonthlyAvailableSub(hasFreeTrialAvailable)
//                } else {
//                    billingService.getAnnualAvailableSub(hasFreeTrialAvailable)
//                }?.let {
//                    planSelected.value = it
//                    initActionButtonForPremiumSelected()
//                    binding.planComposable.setContent {
//                        Column(
//                            modifier = Modifier.padding(
//                                bottom = dimensionResource(R.dimen.margin_normal)
//                            )
//                        ) {
//                            FreePlanView(
//                                isSelected = planSelected.value == null,
//                                isCurrentPlan = !hasActiveRenewingSub.value
//                            ) {
//                                onFreeSelected()
//                            }
//                            PremiumPlanView(
//                                sub = it,
//                                isSelected = planSelected.value == it,
//                                isCurrentPlan = hasActiveRenewingSub.value
//                            ) {
//                                onPremiumSelected(it)
//                            }
//                        }
//                    }
//                }
//            }
//        }

        binding.dialogComposeView.setContent {
            DowngradeDialog(
                shouldShow = shouldShowDowngradeDialog.value,
                onDowngrade = {
                    shouldShowDowngradeDialog.value = false
                    navigator.openSubscriptionInStore(this)
                },
                onClose = {
                    shouldShowDowngradeDialog.value = false
                }
            )
        }

        binding.successBtn.setOnClickListener {
            finish()
        }

        binding.backBtn.setOnClickListener {
            binding.appBar.visible(true)
            binding.topDivider.visible(true)
            // binding.subscriptionTabSelector.visible(true)
            binding.mainContainer.visible(true)
            binding.statusView.visible(false)
            binding.successBtn.visible(false)
            binding.errorButtonsContainer.visible(false)
        }

        binding.retryBtn.setOnClickListener {
            model.getOfferToken()?.let {
                billingService.startBillingFlow(this, it)
            }
        }

        billingService.clearPurchaseUpdates()
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.MANAGE_SUBSCRIPTION, classSimpleName())
    }

    private fun initActionButtonForFreeSelected() {
        if (hasActiveRenewingSub.value) {
            binding.mainActionBtn.text = getString(R.string.downgrade_free_plan)
            styleButton(showSparklesIcon = false, backgroundColor = R.color.warningTint)

            binding.mainActionBtn.setOnClickListener {
                shouldShowDowngradeDialog.value = true
            }
            binding.mainActionBtn.isEnabled = true
        } else {
            binding.mainActionBtn.text = getString(R.string.currently_on_free)
            styleButton(showSparklesIcon = false, backgroundColor = R.color.layer1)
            binding.mainActionBtn.isEnabled = false
        }
    }

    private fun initActionButtonForPremiumSelected() {
        if (hasActiveRenewingSub.value) {
            binding.mainActionBtn.text = getString(R.string.currently_on_premium)
            styleButton(showSparklesIcon = true, backgroundColor = R.color.layer1)
            binding.mainActionBtn.isEnabled = false
        } else {
            binding.mainActionBtn.text = getString(R.string.upgrade_to_premium)
            styleButton(
                showSparklesIcon = true,
                backgroundColor = R.color.crypto,
                textColor = R.color.dark_text
            )

            val planOfferToken = planSelected.value?.offerToken
            binding.mainActionBtn.setOnClickListener {
                if (isLoggedIn && planOfferToken != null) {
                    model.setOfferToken(planOfferToken)
                    billingService.startBillingFlow(this, planOfferToken)
                } else if (!isLoggedIn) {
                    navigator.showLoginDialog(
                        fragmentActivity = this,
                        title = getString(R.string.get_premium),
                        message = getString(R.string.get_premium_login_prompt)
                    )
                }
            }
            binding.mainActionBtn.isEnabled = true
        }
    }

    private fun onFreeSelected() {
        planSelected.value = null
        initActionButtonForFreeSelected()
    }

    private fun onPremiumSelected(subscriptionOffer: SubscriptionOffer?) {
        planSelected.value = subscriptionOffer
        initActionButtonForPremiumSelected()
    }

    private fun styleButton(
        showSparklesIcon: Boolean,
        backgroundColor: Int,
        textColor: Int = R.color.colorOnSurface
    ) {
        val backgroundColor = ContextCompat.getColor(this, backgroundColor)
        val textColor = ContextCompat.getColor(this, textColor)

        binding.mainActionBtn.backgroundTintList = ColorStateList.valueOf(backgroundColor)
        binding.mainActionBtn.setTextColor(textColor)

        if (showSparklesIcon) {
            binding.mainActionBtn.icon = ContextCompat.getDrawable(this, R.drawable.ic_sparkles)
            binding.mainActionBtn.iconTint = ColorStateList.valueOf(textColor)
        } else {
            binding.mainActionBtn.icon = null
        }
    }

    private fun onPurchaseUpdate(state: PurchaseUpdateState) {
        if (state.isLoading) {
            binding.appBar.visible(false)
            binding.topDivider.visible(false)
            binding.mainContainer.visible(false)
            binding.successBtn.visible(false)
            binding.errorButtonsContainer.visible(false)
            binding.statusView.clear().animation(R.raw.anim_loading).visible(true)
        } else if (state.responseCode == BillingResponseCode.USER_CANCELED) {
            binding.statusView.visible(false)
            binding.successBtn.visible(false)
            binding.errorButtonsContainer.visible(false)
            binding.appBar.visible(true)
            binding.topDivider.visible(true)
            //    binding.subscriptionTabSelector.visible(true)
            binding.mainContainer.visible(true)
            billingService.clearPurchaseUpdates()
            analytics.trackEventViewContent(
                AnalyticsService.ParamValue.BILLING_FLOW_RESULT.paramValue,
                success = 0L
            )
        } else if (state.success) {
            binding.appBar.visible(false)
            binding.topDivider.visible(false)
            binding.mainContainer.visible(false)
            binding.errorButtonsContainer.visible(false)
            binding.statusView.clear()
                .animation(R.raw.anim_success)
                .title(R.string.premium_subscription_unlocked)
                .subtitle(R.string.premium_subscription_unlocked_subtitle)
                .visible(true)
            binding.successBtn.visible(true)
            billingService.clearPurchaseUpdates()
            analytics.trackEventViewContent(
                AnalyticsService.ParamValue.BILLING_FLOW_RESULT.paramValue,
                success = 1L
            )
        } else {
            binding.appBar.visible(false)
            binding.topDivider.visible(false)
            binding.mainContainer.visible(false)
            binding.statusView.clear()
                .animation(R.raw.anim_error)
                .title(R.string.purchase_failed)
                .htmlSubtitle(
                    R.string.purchase_failed_message,
                    state.responseCode?.toString() ?: state.debugMessage
                )
                .action(resources.getString(R.string.contact_support_title))
                .listener { navigator.openSupportCenter(this) }
                .visible(true)
            binding.successBtn.visible(false)
            binding.errorButtonsContainer.visible(true)
            billingService.clearPurchaseUpdates()
            analytics.trackEventViewContent(
                AnalyticsService.ParamValue.BILLING_FLOW_RESULT.paramValue,
                success = -1L
            )
        }
    }
}
