package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.UserRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface AuthUseCase {
    suspend fun login(username: String, password: String): Either<Error, String>
    suspend fun getUser(): Either<Failure, User>
}

class AuthUseCaseImpl : AuthUseCase, KoinComponent {
    private val authRepository: AuthRepository by inject()
    private val userRepository: UserRepository by inject()

    override suspend fun login(username: String, password: String): Either<Error, String> {
        return authRepository.login(username, password)
    }

    override suspend fun getUser(): Either<Failure, User> {
        return userRepository.getUser()
    }
}
