package com.weatherxm.usecases

import com.weatherxm.data.repository.UserPreferencesRepository
import org.koin.core.component.KoinComponent

interface AnalyticsOptInUseCase {
    fun setAnalyticsEnabled(enabled: Boolean)
}

class AnalyticsOptInUseCaseImpl(
    private val userPreferencesRepository: UserPreferencesRepository
) : AnalyticsOptInUseCase, KoinComponent {

    override fun setAnalyticsEnabled(enabled: Boolean) {
        userPreferencesRepository.setAnalyticsEnabled(enabled)
    }
}
