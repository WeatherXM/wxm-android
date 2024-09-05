package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.BoostRewardResponse
import com.weatherxm.data.DeviceRewardsSummary
import com.weatherxm.data.Failure
import com.weatherxm.data.RewardDetails
import com.weatherxm.data.Rewards
import com.weatherxm.data.RewardsTimeline
import com.weatherxm.data.WalletRewards
import com.weatherxm.data.datasource.RewardsDataSource
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
    suspend fun getDeviceRewardsSummary(
        deviceId: String,
        mode: RewardsRepositoryImpl.Companion.RewardsSummaryMode
    ): Either<Failure, DeviceRewardsSummary>
}

class RewardsRepositoryImpl(private val dataSource: RewardsDataSource) : RewardsRepository {
    companion object {
        const val TIMELINE_MINUS_MONTHS_TO_FETCH = 3L

        enum class RewardsSummaryMode {
            WEEK,
            MONTH,
            YEAR
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

    override suspend fun getDeviceRewardsSummary(
        deviceId: String,
        mode: RewardsSummaryMode
    ): Either<Failure, DeviceRewardsSummary> {
        return dataSource.getDeviceRewardsSummary(deviceId, mode.name.lowercase())
    }
}
