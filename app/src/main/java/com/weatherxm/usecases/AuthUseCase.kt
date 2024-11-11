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
    suspend fun signup(
        username: String,
        firstName: String?,
        lastName: String?
    ): Either<Failure, String>

    suspend fun resetPassword(email: String): Either<Failure, Unit>
    fun isLoggedIn(): Boolean
    suspend fun isPasswordCorrect(password: String): Either<Failure, Boolean>
    suspend fun logout()
}

class AuthUseCaseImpl(
    private val authRepository: AuthRepository,
    private val notificationsRepository: NotificationsRepository
) : AuthUseCase {

    override fun isLoggedIn(): Boolean {
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

    override suspend fun resetPassword(email: String): Either<Failure, Unit> {
        return authRepository.resetPassword(email)
    }

    override suspend fun isPasswordCorrect(password: String): Either<Failure, Boolean> {
        return authRepository.isPasswordCorrect(password)
    }

    override suspend fun logout() {
        authRepository.logout()
    }
}
