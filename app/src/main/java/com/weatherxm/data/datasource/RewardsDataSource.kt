package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.BoostRewardResponse
import com.weatherxm.data.Failure
import com.weatherxm.data.RewardDetails
import com.weatherxm.data.Rewards
import com.weatherxm.data.RewardsTimeline
import com.weatherxm.data.WalletRewards
import com.weatherxm.data.leftToFailure
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
    suspend fun getRewardDetails(deviceId: String, date: String): Either<Failure, RewardDetails>
    suspend fun getBoostReward(
        deviceId: String,
        boostCode: String
    ): Either<Failure, BoostRewardResponse>

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
        ).leftToFailure()
    }

    override suspend fun getRewards(deviceId: String): Either<Failure, Rewards> {
        return apiService.getRewards(deviceId).leftToFailure()
    }

    override suspend fun getRewardDetails(
        deviceId: String,
        date: String
    ): Either<Failure, RewardDetails> {
        return apiService.getRewardDetails(deviceId, date).leftToFailure()
    }

    override suspend fun getBoostReward(
        deviceId: String,
        boostCode: String
    ): Either<Failure, BoostRewardResponse> {
        return apiService.getRewardBoost(deviceId, boostCode).leftToFailure()
    }

    override suspend fun getWalletRewards(walletAddress: String): Either<Failure, WalletRewards> {
        return apiService.getWalletRewards(walletAddress).leftToFailure()
    }
}
