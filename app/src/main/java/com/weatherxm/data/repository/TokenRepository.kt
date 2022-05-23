package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.TokensSummaryResponse
import com.weatherxm.data.TransactionsResponse
import com.weatherxm.data.datasource.TokenDataSource

interface TokenRepository {
    suspend fun getTokens24H(deviceId: String, forceRefresh: Boolean): Either<Failure, Float?>
    suspend fun getTokens7D(
        deviceId: String,
        forceRefresh: Boolean
    ): Either<Failure, TokensSummaryResponse>

    suspend fun getTokens30D(
        deviceId: String,
        forceRefresh: Boolean
    ): Either<Failure, TokensSummaryResponse>

    suspend fun getTransactions(
        deviceId: String,
        page: Int?
    ): Either<Failure, TransactionsResponse>
}

class TokenRepositoryImpl(private val tokenDataSource: TokenDataSource) : TokenRepository {

    override suspend fun getTokens24H(
        deviceId: String,
        forceRefresh: Boolean
    ): Either<Failure, Float?> {
        return tokenDataSource.getTokens(deviceId, forceRefresh).map {
            it.daily.total
        }
    }

    override suspend fun getTokens7D(
        deviceId: String,
        forceRefresh: Boolean
    ): Either<Failure, TokensSummaryResponse> {
        return tokenDataSource.getTokens(deviceId, forceRefresh).map {
            it.weekly
        }
    }

    override suspend fun getTokens30D(
        deviceId: String,
        forceRefresh: Boolean
    ): Either<Failure, TokensSummaryResponse> {
        return tokenDataSource.getTokens(deviceId, forceRefresh).map {
            it.monthly
        }
    }

    override suspend fun getTransactions(
        deviceId: String,
        page: Int?
    ): Either<Failure, TransactionsResponse> {
        return tokenDataSource.getTransactions(deviceId, page)
    }
}
