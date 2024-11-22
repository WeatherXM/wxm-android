package com.weatherxm.usecases

import com.weatherxm.data.repository.AppConfigRepository
import com.weatherxm.data.repository.UserPreferencesRepository

interface PreferencesUseCase {
    fun setAnalyticsEnabled(enabled: Boolean)
    fun getInstallationId(): String?
}

class PreferencesUseCaseImpl(
    private val appConfigRepository: AppConfigRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : PreferencesUseCase {

    override fun setAnalyticsEnabled(enabled: Boolean) {
        userPreferencesRepository.setAnalyticsEnabled(enabled)
    }

    override fun getInstallationId(): String? {
        return appConfigRepository.getInstallationId()
    }
}
