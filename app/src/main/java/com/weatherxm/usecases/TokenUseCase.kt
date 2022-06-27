package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.TokenRepository
import com.weatherxm.ui.UITransactions
import com.weatherxm.util.DateTimeHelper.getTimezone

interface TokenUseCase {
    suspend fun getTransactions(
        deviceId: String,
        page: Int?,
        fromDate: String?,
        toDate: String? = null
    ): Either<Failure, UITransactions>
}

class TokenUseCaseImpl(private val tokenRepository: TokenRepository) : TokenUseCase {

    override suspend fun getTransactions(
        deviceId: String,
        page: Int?,
        fromDate: String?,
        toDate: String?
    ): Either<Failure, UITransactions> {
        return tokenRepository.getTransactions(deviceId, page, getTimezone(), fromDate, toDate)
            .map {
                if (it.data.isEmpty()) {
                    UITransactions(listOf(), reachedTotal = true)
                } else {
                    val filteredTxs = it.data.filter { tx -> tx.actualReward != null }
                    UITransactions(filteredTxs, it.hasNextPage)
                }
            }
    }
}
