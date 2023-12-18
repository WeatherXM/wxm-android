package com.weatherxm.ui.rewarddetails

import androidx.lifecycle.ViewModel
import com.weatherxm.usecases.AppConfigUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RewardDetailsViewModel : ViewModel(), KoinComponent {
    private val appConfigUseCase: AppConfigUseCase by inject()

    fun isTokenClaimingEnabled() = appConfigUseCase.isTokenClaimingEnabled()

    fun isPoLEnabled() = appConfigUseCase.isPoLEnabled()
}
