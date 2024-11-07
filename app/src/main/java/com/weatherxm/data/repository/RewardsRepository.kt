package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.datasource.RewardsDataSource
import com.weatherxm.data.models.BoostRewardResponse
import com.weatherxm.data.models.DeviceRewardsSummary
import com.weatherxm.data.models.DevicesRewards
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.RewardDetails
import com.weatherxm.data.models.Rewards
import com.weatherxm.data.models.RewardsTimeline
import com.weatherxm.data.models.WalletRewards
import com.weatherxm.util.toISODate
import java.time.ZoneId
import java.time.ZonedDateTime

interface RewardsRepository {
    suspend fun getRewardsTimeline(
        deviceId: String,
        page: Int?
    ): Either<Failure, RewardsTimeline>

    suspend fun getRewards(deviceId: String): Either<Failure, Rewards>
    suspend fun getRewardDetails(
        deviceId: String,
        date: ZonedDateTime
    ): Either<Failure, RewardDetails>

    suspend fun getBoostReward(
        deviceId: String,
        boostCode: String
    ): Either<Failure, BoostRewardResponse>

    suspend fun getWalletRewards(walletAddress: String): Either<Failure, WalletRewards>
    suspend fun getDevicesRewardsByRange(
        mode: RewardsRepositoryImpl.Companion.RewardsSummaryMode
    ): Either<Failure, DevicesRewards>

    suspend fun getDeviceRewardsByRange(
        deviceId: String,
        mode: RewardsRepositoryImpl.Companion.RewardsSummaryMode
    ): Either<Failure, DeviceRewardsSummary>
}

class RewardsRepositoryImpl(private val dataSource: RewardsDataSource) : RewardsRepository {
    companion object {
        const val TIMELINE_MINUS_MONTHS_TO_FETCH = 3L

        enum class RewardsSummaryMode {
            WEEK,
            MONTH
        }
    }

    override suspend fun getRewardsTimeline(
        deviceId: String,
        page: Int?
    ): Either<Failure, RewardsTimeline> {
        return dataSource.getRewardsTimeline(
            deviceId,
            page,
            timezone = ZoneId.of("UTC").toString(),
            fromDate = ZonedDateTime.now().minusMonths(TIMELINE_MINUS_MONTHS_TO_FETCH).toISODate()
        )
    }

    override suspend fun getRewards(deviceId: String): Either<Failure, Rewards> {
        return dataSource.getRewards(deviceId)
    }

    override suspend fun getRewardDetails(
        deviceId: String,
        date: ZonedDateTime
    ): Either<Failure, RewardDetails> {
        return dataSource.getRewardDetails(deviceId, date.toISODate())
    }

    override suspend fun getBoostReward(
        deviceId: String,
        boostCode: String
    ): Either<Failure, BoostRewardResponse> {
        return dataSource.getBoostReward(deviceId, boostCode)
    }

    override suspend fun getWalletRewards(walletAddress: String): Either<Failure, WalletRewards> {
        return dataSource.getWalletRewards(walletAddress)
    }

    override suspend fun getDevicesRewardsByRange(
        mode: RewardsSummaryMode
    ): Either<Failure, DevicesRewards> {
        return dataSource.getDevicesRewardsByRange(mode.name.lowercase())
    }

    override suspend fun getDeviceRewardsByRange(
        deviceId: String,
        mode: RewardsSummaryMode
    ): Either<Failure, DeviceRewardsSummary> {
        return dataSource.getDeviceRewardsByRange(deviceId, mode.name.lowercase())
    }
}
