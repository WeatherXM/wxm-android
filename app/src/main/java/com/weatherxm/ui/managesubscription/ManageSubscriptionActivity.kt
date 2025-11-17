package com.weatherxm.ui.managesubscription

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityManageSubscriptionBinding
import com.weatherxm.service.BillingService
import com.weatherxm.ui.common.Contracts.ARG_HAS_FREE_TRIAL_AVAILABLE
import com.weatherxm.ui.common.PurchaseUpdateState
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ManageSubscriptionActivity : BaseActivity() {
    private lateinit var binding: ActivityManageSubscriptionBinding
    private val model: ManageSubscriptionViewModel by viewModel()
    private val billingService: BillingService by inject()

    private var hasFreeTrialAvailable = false

    init {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                billingService.getPurchaseUpdates().collect {
                    onPurchaseUpdate(it)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageSubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hasFreeTrialAvailable = intent?.extras?.getBoolean(ARG_HAS_FREE_TRIAL_AVAILABLE) == true

        with(binding.toolbar) {
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }

        binding.currentPlanComposable.setContent {
            CurrentPlanView(billingService.getActiveSub()) {
                navigator.openSubscriptionInStore(this)
            }
        }

        binding.premiumFeaturesComposable.setContent {
            PremiumFeaturesView {
                binding.selectPlanComposable.visible(true)
                binding.premiumFeaturesComposable.visible(false)
                binding.currentPlanComposable.visible(false)
            }
        }

        binding.selectPlanComposable.setContent {
            PlansView(billingService.getAvailableSubs(hasFreeTrialAvailable)) { offer ->
                offer?.let {
                    model.setOfferToken(it.offerToken)
                    billingService.startBillingFlow(this, it.offerToken)
                }
            }
        }

        binding.successBtn.setOnClickListener {
            finish()
        }

        binding.backBtn.setOnClickListener {
            binding.appBar.visible(true)
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
        binding.currentPlanComposable.visible(true)
        binding.premiumFeaturesComposable.visible(!billingService.hasActiveSub())
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.MANAGE_SUBSCRIPTION, classSimpleName())
    }

    private fun onPurchaseUpdate(state: PurchaseUpdateState) {
        if (state.isLoading) {
            binding.appBar.visible(false)
            binding.mainContainer.visible(false)
            binding.successBtn.visible(false)
            binding.errorButtonsContainer.visible(false)
            binding.statusView.clear().animation(R.raw.anim_loading).visible(true)
        } else if (!state.success && state.responseCode == BillingResponseCode.USER_CANCELED) {
            binding.appBar.visible(true)
            binding.mainContainer.visible(true)
            binding.statusView.visible(false)
            binding.successBtn.visible(false)
            binding.errorButtonsContainer.visible(false)
        } else if (state.success) {
            binding.appBar.visible(false)
            binding.mainContainer.visible(false)
            binding.statusView.clear()
                .animation(R.raw.anim_success)
                .title(R.string.premium_subscription_unlocked)
                .subtitle(R.string.premium_subscription_unlocked_subtitle)
                .visible(true)
            binding.successBtn.visible(true)
        } else {
            binding.appBar.visible(false)
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
            binding.errorButtonsContainer.visible(true)
        }
    }
}
