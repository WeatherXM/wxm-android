package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.User
import com.weatherxm.data.network.AuthToken
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.NotificationsRepository
import com.weatherxm.data.repository.UserPreferencesRepository
import com.weatherxm.data.repository.UserRepository

interface AuthUseCase {
    suspend fun login(username: String, password: String): Either<Failure, AuthToken>
    suspend fun getUser(): Either<Failure, User>
    suspend fun signup(
        username: String,
        firstName: String?,
        lastName: String?
    ): Either<Failure, String>

    suspend fun resetPassword(email: String): Either<Failure, Unit>
    suspend fun isLoggedIn(): Either<Failure, Boolean>
    fun shouldShowAnalyticsOptIn(): Boolean
    suspend fun isPasswordCorrect(password: String): Either<Failure, Boolean>
}

class AuthUseCaseImpl(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val notificationsRepository: NotificationsRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : AuthUseCase {

    override suspend fun isLoggedIn(): Either<Failure, Boolean> {
        return authRepository.isLoggedIn()
    }

    override suspend fun signup(
        username: String,
        firstName: String?,
        lastName: String?
    ): Either<Failure, String> {
        return authRepository.signup(username, firstName, lastName).map {
            username
        }
    }

    override suspend fun login(username: String, password: String): Either<Failure, AuthToken> {
        return authRepository.login(username, password).onRight {
            notificationsRepository.setFcmToken()
        }
    }

    override suspend fun getUser(): Either<Failure, User> {
        return userRepository.getUser()
    }

    override suspend fun resetPassword(email: String): Either<Failure, Unit> {
        return authRepository.resetPassword(email)
    }

    override fun shouldShowAnalyticsOptIn(): Boolean {
        return userPreferencesRepository.shouldShowAnalyticsOptIn()
    }

    override suspend fun isPasswordCorrect(password: String): Either<Failure, Boolean> {
        return authRepository.isPasswordCorrect(password)
    }
}
