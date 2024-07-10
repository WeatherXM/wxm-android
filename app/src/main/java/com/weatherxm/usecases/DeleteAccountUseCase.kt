package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.NotificationsRepository
import com.weatherxm.data.repository.UserRepository

interface DeleteAccountUseCase {
    suspend fun isPasswordCorrect(password: String): Either<Failure, Boolean>
    suspend fun deleteAccount(): Either<Failure, Unit>
}

class DeleteAccountUseCaseImpl(
    private val userRepository: UserRepository,
    private val notificationsRepository: NotificationsRepository,
    private val authRepository: AuthRepository
) : DeleteAccountUseCase {
    override suspend fun isPasswordCorrect(password: String): Either<Failure, Boolean> {
        return authRepository.isPasswordCorrect(password)
    }

    override suspend fun deleteAccount(): Either<Failure, Unit> {
        return userRepository.deleteAccount().onRight {
            notificationsRepository.deleteFcmToken()
            authRepository.logout()
        }
    }
}
