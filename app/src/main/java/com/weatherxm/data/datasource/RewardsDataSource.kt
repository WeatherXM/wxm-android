package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.Rewards
import com.weatherxm.data.RewardsTimeline
import com.weatherxm.data.WalletRewards
import com.weatherxm.data.map
import com.weatherxm.data.network.ApiService

interface RewardsDataSource {
    @Suppress("LongParameterList")
    suspend fun getRewardsTimeline(
        deviceId: String,
        page: Int?,
        pageSize: Int? = null,
        timezone: String? = null,
        fromDate: String? = null,
        toDate: String? = null
    ): Either<Failure, RewardsTimeline>

    suspend fun getRewards(deviceId: String): Either<Failure, Rewards>
    suspend fun getWalletRewards(walletAddress: String): Either<Failure, WalletRewards>
}

class RewardsDataSourceImpl(private val apiService: ApiService) : RewardsDataSource {
    override suspend fun getRewardsTimeline(
        deviceId: String,
        page: Int?,
        pageSize: Int?,
        timezone: String?,
        fromDate: String?,
        toDate: String?
    ): Either<Failure, RewardsTimeline> {
        return apiService.getRewardsTimeline(
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

    override suspend fun getWalletRewards(walletAddress: String): Either<Failure, WalletRewards> {
        return apiService.getWalletRewards(walletAddress).map()
    }
}
