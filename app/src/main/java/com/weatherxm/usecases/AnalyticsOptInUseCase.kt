package com.weatherxm.usecases

import com.weatherxm.data.repository.UserPreferencesRepository

interface AnalyticsOptInUseCase {
    fun setAnalyticsEnabled(enabled: Boolean)
}

class AnalyticsOptInUseCaseImpl(
    private val userPreferencesRepository: UserPreferencesRepository
) : AnalyticsOptInUseCase {

    override fun setAnalyticsEnabled(enabled: Boolean) {
        userPreferencesRepository.setAnalyticsEnabled(enabled)
    }
}
