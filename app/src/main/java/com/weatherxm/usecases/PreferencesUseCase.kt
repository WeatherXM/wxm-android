package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.UserRepository
import com.weatherxm.data.repository.WalletRepository
import com.weatherxm.data.repository.WeatherForecastRepository

interface PreferencesUseCase {
    suspend fun isLoggedIn(): Either<Error, String>
    suspend fun logout()
    fun hasDismissedSurveyPrompt(): Boolean
    fun dismissSurveyPrompt()
}

class PreferencesUseCaseImpl(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val weatherForecastRepository: WeatherForecastRepository,
    private val walletRepository: WalletRepository,
) : PreferencesUseCase {

    override suspend fun isLoggedIn(): Either<Error, String> {
        return authRepository.isLoggedIn()
    }

    override suspend fun logout() {
        authRepository.logout()
        userRepository.clearCache()
        weatherForecastRepository.clearCache()
        walletRepository.clearCache()
    }

    override fun hasDismissedSurveyPrompt(): Boolean {
        return userRepository.hasDismissedSurveyPrompt()
    }

    override fun dismissSurveyPrompt() {
        return userRepository.dismissSurveyPrompt()
    }
}
