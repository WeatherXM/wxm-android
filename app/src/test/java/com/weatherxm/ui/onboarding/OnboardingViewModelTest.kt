package com.weatherxm.ui.onboarding

import com.weatherxm.usecases.StartupUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class OnboardingViewModelTest : BehaviorSpec({
    val usecase = mockk<StartupUseCase>()
    val viewModel = OnboardingViewModel(usecase)

    beforeSpec {
        justRun { usecase.disableShouldShowOnboarding() }
    }

    context("Disable that we should show the onboarding") {
        When("Using the view model to disable that flag") {
            viewModel.disableShouldShowOnboarding()
            then("verify that the call to disable that flag is made") {
                verify(exactly = 1) { usecase.disableShouldShowOnboarding() }
            }
        }
    }
})
