package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.Tokens
import com.weatherxm.data.map
import com.weatherxm.data.network.ApiService

interface TokenDataSource {
    suspend fun getTokensSummary(deviceId: String): Either<Failure, Tokens>
}

class TokenDataSourceImpl(
    private val apiService: ApiService
) : TokenDataSource {

    override suspend fun getTokensSummary(deviceId: String): Either<Failure, Tokens> {
        return apiService.getTokensSummary(deviceId).map()
    }
}
