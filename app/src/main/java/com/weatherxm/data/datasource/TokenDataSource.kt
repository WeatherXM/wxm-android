package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.Tokens
import com.weatherxm.data.TransactionsResponse
import com.weatherxm.data.map
import com.weatherxm.data.network.ApiService

interface TokenDataSource {
    suspend fun getTokens(deviceId: String, forceRefresh: Boolean): Either<Failure, Tokens>
    suspend fun getTransactions(deviceId: String, page: Int?): Either<Failure, TransactionsResponse>
}

class TokenDataSourceImpl(
    private val apiService: ApiService
) : TokenDataSource {
    lateinit var tokens: Tokens

    override suspend fun getTokens(
        deviceId: String,
        forceRefresh: Boolean
    ): Either<Failure, Tokens> {
        return if (this::tokens.isInitialized && !forceRefresh) {
            Either.Right(tokens)
        } else {
            apiService.getTokens(deviceId).map().tap {
                tokens = it
            }
        }
    }

    override suspend fun getTransactions(
        deviceId: String,
        page: Int?
    ): Either<Failure, TransactionsResponse> {
        return apiService.getTransactions(deviceId, page).map()
    }
}
