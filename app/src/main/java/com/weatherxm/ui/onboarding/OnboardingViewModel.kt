package com.weatherxm.ui.onboarding

import androidx.lifecycle.ViewModel
import com.weatherxm.usecases.StartupUseCase

class OnboardingViewModel(private val usecase: StartupUseCase) : ViewModel() {

    fun disableShouldShowOnboarding() {
        usecase.disableShouldShowOnboarding()
    }
}
