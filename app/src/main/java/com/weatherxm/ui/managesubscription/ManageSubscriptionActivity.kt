package com.weatherxm.ui.managesubscription

import android.os.Bundle
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityManageSubscriptionBinding
import com.weatherxm.service.BillingService
import com.weatherxm.ui.common.Contracts.ARG_HAS_FREE_TRIAL_AVAILABLE
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import org.koin.android.ext.android.inject

class ManageSubscriptionActivity : BaseActivity() {
    private lateinit var binding: ActivityManageSubscriptionBinding
    private val billingService: BillingService by inject()

    private var hasFreeTrialAvailable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageSubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hasFreeTrialAvailable = intent?.extras?.getBoolean(ARG_HAS_FREE_TRIAL_AVAILABLE) == true

        with(binding.toolbar) {
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }

        binding.currentPlanComposable.setContent {
            CurrentPlanView(billingService.getActiveSub())
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
                    billingService.startBillingFlow(this, it.offerToken)
                }
            }
        }

        binding.currentPlanComposable.visible(true)
        binding.premiumFeaturesComposable.visible(!billingService.hasActiveSub())
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.MANAGE_SUBSCRIPTION, classSimpleName())
    }
}
