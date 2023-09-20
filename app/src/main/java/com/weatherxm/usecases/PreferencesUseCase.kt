package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.UserPreferencesRepository

interface PreferencesUseCase {
    suspend fun isLoggedIn(): Either<Failure, Boolean>
    suspend fun logout()
    fun hasDismissedSurveyPrompt(): Boolean
    fun dismissSurveyPrompt()
    fun setAnalyticsEnabled(enabled: Boolean)
}

class PreferencesUseCaseImpl(
    private val authRepository: AuthRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : PreferencesUseCase {

    override suspend fun isLoggedIn(): Either<Failure, Boolean> {
        return authRepository.isLoggedIn()
    }

    override suspend fun logout() {
        authRepository.logout()
    }

    override fun hasDismissedSurveyPrompt(): Boolean {
        return userPreferencesRepository.hasDismissedSurveyPrompt()
    }

    override fun dismissSurveyPrompt() {
        return userPreferencesRepository.dismissSurveyPrompt()
    }

    override fun setAnalyticsEnabled(enabled: Boolean) {
        return userPreferencesRepository.setAnalyticsEnabled(enabled)
    }
}
