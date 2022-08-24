package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.UserRepository
import com.weatherxm.data.repository.WalletRepository
import com.weatherxm.data.repository.WeatherForecastRepository

interface AuthUseCase {
    suspend fun login(username: String, password: String): Either<Failure, String>
    suspend fun getUser(): Either<Failure, User>
    suspend fun signup(
        username: String,
        firstName: String?,
        lastName: String?
    ): Either<Failure, String>

    suspend fun resetPassword(email: String): Either<Failure, Unit>
    suspend fun isLoggedIn(): Either<Error, String>
    suspend fun logout()
}

class AuthUseCaseImpl(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val weatherForecastRepository: WeatherForecastRepository,
    private val walletRepository: WalletRepository
) : AuthUseCase {

    override suspend fun isLoggedIn(): Either<Error, String> {
        return authRepository.isLoggedIn()
    }

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

    override suspend fun logout() {
        authRepository.logout()
        userRepository.clearCache()
        weatherForecastRepository.clearCache()
        walletRepository.clearCache()
    }
}
