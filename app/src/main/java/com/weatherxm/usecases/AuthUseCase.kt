package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.UserRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface AuthUseCase {
    suspend fun login(username: String, password: String): Either<Failure, String>
    suspend fun getUser(): Either<Failure, User>
    suspend fun signup(
        username: String,
        firstName: String?,
        lastName: String?
    ): Either<Failure, String>

    suspend fun resetPassword(email: String): Either<Failure, Unit>
}

class AuthUseCaseImpl : AuthUseCase, KoinComponent {
    private val authRepository: AuthRepository by inject()
    private val userRepository: UserRepository by inject()

    override suspend fun signup(
        username: String,
        firstName: String?,
        lastName: String?
    ): Either<Failure, String> {
        return authRepository.signup(username, firstName, lastName)
    }

    override suspend fun login(username: String, password: String): Either<Failure, String> {
        return authRepository.login(username, password)
    }

    override suspend fun getUser(): Either<Failure, User> {
        return userRepository.getUser()
    }

    override suspend fun resetPassword(email: String): Either<Failure, Unit> {
        return authRepository.resetPassword(email)
    }
}
