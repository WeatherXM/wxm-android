package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.TokenEntry
import com.weatherxm.data.Tokens
import com.weatherxm.data.TokensSummaryResponse
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
        // TODO Perform actual network call here when we have a working endpoint
        // return apiService.getTokens(deviceId).map()
        return Either.Right(
            Tokens(
                lastDayActualReward = 1.0F,
                weekly = TokensSummaryResponse(7F, List(7) { TokenEntry("", 1F) }),
                monthly = TokensSummaryResponse(30F, List(30) { TokenEntry("", 1F) })
            )
        )
    }

    override suspend fun getTransactions(deviceId: String): Either<Failure, List<Transaction>> {
        return apiService.getTransactions(deviceId).map()
    }
}
