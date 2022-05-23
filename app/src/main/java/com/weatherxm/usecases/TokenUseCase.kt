package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.TokenRepository
import com.weatherxm.ui.UITransactions

interface TokenUseCase {
    suspend fun getTransactions(deviceId: String, page: Int?): Either<Failure, UITransactions>
}

class TokenUseCaseImpl(private val tokenRepository: TokenRepository) : TokenUseCase {

    override suspend fun getTransactions(
        deviceId: String,
        page: Int?
    ): Either<Failure, UITransactions> {
        return tokenRepository.getTransactions(deviceId, page)
            .map {
                val filteredTxs = it.data.filter { transaction -> transaction.actualReward != null }
                UITransactions(filteredTxs, it.hasNextPage)
            }
    }
}
