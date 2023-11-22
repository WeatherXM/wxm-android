package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.Rewards
import com.weatherxm.data.RewardsObject
import com.weatherxm.data.TransactionsResponse
import com.weatherxm.data.map
import com.weatherxm.data.network.ApiService

interface RewardsDataSource {
    @Suppress("LongParameterList")
    suspend fun getTransactions(
        deviceId: String,
        page: Int?,
        pageSize: Int? = null,
        timezone: String? = null,
        fromDate: String? = null,
        toDate: String? = null
    ): Either<Failure, TransactionsResponse>

    suspend fun getRewards(deviceId: String): Either<Failure, Rewards>
    suspend fun getRewardDetails(deviceId: String, txHash: String): Either<Failure, RewardsObject>
}

class RewardsDataSourceImpl(private val apiService: ApiService) : RewardsDataSource {
    override suspend fun getTransactions(
        deviceId: String,
        page: Int?,
        pageSize: Int?,
        timezone: String?,
        fromDate: String?,
        toDate: String?
    ): Either<Failure, TransactionsResponse> {
        return apiService.getTransactions(
            deviceId,
            page,
            pageSize,
            timezone,
            fromDate,
            toDate
        ).map()
    }

    override suspend fun getRewards(deviceId: String): Either<Failure, Rewards> {
        return apiService.getRewards(deviceId).map()
    }

    override suspend fun getRewardDetails(
        deviceId: String,
        txHash: String
    ): Either<Failure, RewardsObject> {
        return apiService.getRewardDetails(deviceId, txHash).map()
    }
}
