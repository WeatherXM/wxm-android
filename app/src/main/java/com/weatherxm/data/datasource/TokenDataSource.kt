package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.TransactionsResponse
import com.weatherxm.data.map
import com.weatherxm.data.network.ApiService

interface TokenDataSource {
    @Suppress("LongParameterList")
    suspend fun getTransactions(
        deviceId: String,
        page: Int?,
        pageSize: Int? = null,
        timezone: String? = null,
        fromDate: String? = null,
        toDate: String? = null
    ): Either<Failure, TransactionsResponse>
}

class TokenDataSourceImpl(private val apiService: ApiService) : TokenDataSource {
    override suspend fun getTransactions(
        deviceId: String,
        page: Int?,
        pageSize: Int?,
        timezone: String?,
        fromDate: String?,
        toDate: String?
    ): Either<Failure, TransactionsResponse> {
        return apiService.getTransactions(deviceId, page, pageSize, timezone, fromDate, toDate)
            .map()
    }
}
