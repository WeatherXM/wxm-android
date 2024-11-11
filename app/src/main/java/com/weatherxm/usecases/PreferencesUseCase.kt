package com.weatherxm.usecases

import com.weatherxm.data.repository.AppConfigRepository
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.UserPreferencesRepository

interface PreferencesUseCase {
    fun isLoggedIn(): Boolean
    suspend fun logout()
    fun setAnalyticsEnabled(enabled: Boolean)
    fun getInstallationId(): String?
}

class PreferencesUseCaseImpl(
    private val authRepository: AuthRepository,
    private val appConfigRepository: AppConfigRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : PreferencesUseCase {

    override fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }

    override suspend fun logout() {
        authRepository.logout()
    }

    override fun setAnalyticsEnabled(enabled: Boolean) {
        userPreferencesRepository.setAnalyticsEnabled(enabled)
    }

    override fun getInstallationId(): String? {
        return appConfigRepository.getInstallationId()
    }
}
