package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.TokensSummaryResponse
import com.weatherxm.data.Transaction
import com.weatherxm.data.datasource.TokenDataSource
import org.koin.core.component.KoinComponent

class TokenRepository(private val tokenDataSource: TokenDataSource) : KoinComponent {

    suspend fun getTokens24H(deviceId: String): Either<Failure, Float?> {
        return tokenDataSource.getTokens(deviceId).map {
            it.last_day_actual_reward
        }
    }

    suspend fun getTokens7D(deviceId: String): Either<Failure, TokensSummaryResponse> {
        return tokenDataSource.getTokens(deviceId).map {
            it.weekly
        }
    }

    suspend fun getTokens30D(deviceId: String): Either<Failure, TokensSummaryResponse> {
        return tokenDataSource.getTokens(deviceId).map {
            it.monthly
        }
    }

    suspend fun getTransactions(deviceId: String): Either<Failure, List<Transaction>> {
        return tokenDataSource.getTransactions(deviceId)
    }
}
