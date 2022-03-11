package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.Transaction
import com.weatherxm.data.repository.TokenRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface TokenUseCase {
    suspend fun getTransactions(
        deviceId: String,
        page: Int?,
        pageSize: Int?
    ): Either<Failure, List<Transaction>>
}

class TokenUseCaseImpl : TokenUseCase, KoinComponent {
    private val tokenRepository: TokenRepository by inject()

    override suspend fun getTransactions(
        deviceId: String,
        page: Int?,
        pageSize: Int?
    ): Either<Failure, List<Transaction>> {
        return tokenRepository.getTransactions(deviceId, page, pageSize)
    }
}
