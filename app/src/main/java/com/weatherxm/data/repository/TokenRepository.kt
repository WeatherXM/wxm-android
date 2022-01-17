package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.TokensSummaryResponse
import com.weatherxm.data.datasource.TokenDataSource
import org.koin.core.component.KoinComponent

class TokenRepository(private val tokenDataSource: TokenDataSource) : KoinComponent {

    suspend fun getTokensSummary24H(deviceId: String): Either<Failure, TokensSummaryResponse> {
        return tokenDataSource.getTokensSummary(deviceId).map {
            it.token24hour
        }
    }

    suspend fun getTokensSummary7D(deviceId: String): Either<Failure, TokensSummaryResponse> {
        return tokenDataSource.getTokensSummary(deviceId).map {
            it.token7days
        }
    }

    suspend fun getTokensSummary30D(deviceId: String): Either<Failure, TokensSummaryResponse> {
        return tokenDataSource.getTokensSummary(deviceId).map {
            it.token30days
        }
    }
}
