package com.weatherxm.data.repository

import arrow.core.Either
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
        page: Int?,
        timezone: String? = null,
        fromDate: String?,
        toDate: String? = null
    ): Either<Failure, RewardsTimeline>

    suspend fun getRewards(deviceId: String): Either<Failure, Rewards>
    suspend fun getRewardDetails(
        deviceId: String,
        date: ZonedDateTime
    ): Either<Failure, RewardDetails>

    suspend fun getWalletRewards(walletAddress: String): Either<Failure, WalletRewards>
}

class RewardsRepositoryImpl(private val dataSource: RewardsDataSource) : RewardsRepository {

    override suspend fun getRewardsTimeline(
        deviceId: String,
        page: Int?,
        timezone: String?,
        fromDate: String?,
        toDate: String?
    ): Either<Failure, RewardsTimeline> {
        return dataSource.getRewardsTimeline(
            deviceId,
            page,
            timezone = ZoneId.of("UTC").toString(),
            fromDate = fromDate,
            toDate = toDate,
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

    override suspend fun getWalletRewards(walletAddress: String): Either<Failure, WalletRewards> {
        return dataSource.getWalletRewards(walletAddress)
    }
}
