package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.TokenRepository
import com.weatherxm.ui.UITransactions
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface TokenUseCase {
    suspend fun getTransactions(deviceId: String, page: Int?): Either<Failure, UITransactions>
}

class TokenUseCaseImpl : TokenUseCase, KoinComponent {
    private val tokenRepository: TokenRepository by inject()

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
