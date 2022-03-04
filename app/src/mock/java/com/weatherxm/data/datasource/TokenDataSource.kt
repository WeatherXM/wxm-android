package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.Tokens
import com.weatherxm.data.Transaction
import com.weatherxm.data.map
import com.weatherxm.data.network.ApiService

interface TokenDataSource {
    suspend fun getTokens(deviceId: String): Either<Failure, Tokens>
    suspend fun getTransactions(deviceId: String): Either<Failure, List<Transaction>>
}

class TokenDataSourceImpl(
    private val apiService: ApiService
) : TokenDataSource {

    override suspend fun getTokens(deviceId: String): Either<Failure, Tokens> {
        return apiService.getTokens(deviceId).map()
    }

    override suspend fun getTransactions(deviceId: String): Either<Failure, List<Transaction>> {
        return apiService.getTransactions(deviceId).map()
    }
}
