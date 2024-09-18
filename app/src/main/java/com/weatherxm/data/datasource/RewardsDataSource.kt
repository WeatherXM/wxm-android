package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.BoostRewardResponse
import com.weatherxm.data.DeviceRewardsSummary
import com.weatherxm.data.DevicesRewards
import com.weatherxm.data.Failure
import com.weatherxm.data.RewardDetails
import com.weatherxm.data.Rewards
import com.weatherxm.data.RewardsTimeline
import com.weatherxm.data.WalletRewards
import com.weatherxm.data.mapResponse
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
    suspend fun getDevicesRewardsByRange(mode: String): Either<Failure, DevicesRewards>
    suspend fun getDeviceRewardsByRange(
        deviceId: String,
        mode: String
    ): Either<Failure, DeviceRewardsSummary>
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
        ).mapResponse()
    }

    override suspend fun getRewards(deviceId: String): Either<Failure, Rewards> {
        return apiService.getRewards(deviceId).mapResponse()
    }

    override suspend fun getRewardDetails(
        deviceId: String,
        date: String
    ): Either<Failure, RewardDetails> {
        return apiService.getRewardDetails(deviceId, date).mapResponse()
    }

    override suspend fun getBoostReward(
        deviceId: String,
        boostCode: String
    ): Either<Failure, BoostRewardResponse> {
        return apiService.getRewardBoost(deviceId, boostCode).mapResponse()
    }

    override suspend fun getWalletRewards(walletAddress: String): Either<Failure, WalletRewards> {
        return apiService.getWalletRewards(walletAddress).mapResponse()
    }

    override suspend fun getDevicesRewardsByRange(mode: String): Either<Failure, DevicesRewards> {
        return apiService.getDevicesRewardsByRange(mode).mapResponse()
    }

    override suspend fun getDeviceRewardsByRange(
        deviceId: String,
        mode: String
    ): Either<Failure, DeviceRewardsSummary> {
        return apiService.getDeviceRewardsByRange(deviceId, mode).mapResponse()
    }
}
