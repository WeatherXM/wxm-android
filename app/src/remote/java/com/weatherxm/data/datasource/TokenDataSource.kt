package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.TokenEntry
import com.weatherxm.data.Tokens
import com.weatherxm.data.TokensSummaryResponse
import com.weatherxm.data.network.ApiService

interface TokenDataSource {
    suspend fun getTokensSummary(deviceId: String): Either<Failure, Tokens>
}

class TokenDataSourceImpl(
    private val apiService: ApiService
) : TokenDataSource {

    override suspend fun getTokensSummary(deviceId: String): Either<Failure, Tokens> {
        // TODO Perform actual network call here when we have a working endpoint
        // return apiService.getTokensSummary(deviceId).map()
        return Either.Right(
            Tokens(
                token24hour = TokensSummaryResponse(1F, List(24) { TokenEntry("", 1F / 24F) }),
                token7days = TokensSummaryResponse(7F, List(7) { TokenEntry("", 1F) }),
                token30days = TokensSummaryResponse(30F, List(30) { TokenEntry("", 1F) })
            )
        )
    }
}
