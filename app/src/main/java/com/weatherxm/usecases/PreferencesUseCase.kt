package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.UserRepository

interface PreferencesUseCase {
    suspend fun isLoggedIn(): Either<Failure, Boolean>
    suspend fun logout()
    fun hasDismissedSurveyPrompt(): Boolean
    fun dismissSurveyPrompt()
}

class PreferencesUseCaseImpl(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : PreferencesUseCase {

    override suspend fun isLoggedIn(): Either<Failure, Boolean> {
        return authRepository.isLoggedIn()
    }

    override suspend fun logout() {
        authRepository.logout()
    }

    override fun hasDismissedSurveyPrompt(): Boolean {
        return userRepository.hasDismissedSurveyPrompt()
    }

    override fun dismissSurveyPrompt() {
        return userRepository.dismissSurveyPrompt()
    }
}
