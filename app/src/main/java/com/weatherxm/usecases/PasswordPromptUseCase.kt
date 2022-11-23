package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.AuthRepository

interface PasswordPromptUseCase {
    suspend fun isPasswordCorrect(password: String): Either<Failure, Unit>
}

class PasswordPromptUseCaseImpl(
    private val authRepository: AuthRepository
) : PasswordPromptUseCase {

    override suspend fun isPasswordCorrect(password: String): Either<Failure, Unit> {
        return authRepository.isPasswordCorrect(password).map { }
    }
}
