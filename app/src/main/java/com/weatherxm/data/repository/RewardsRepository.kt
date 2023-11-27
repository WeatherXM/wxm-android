package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.Rewards
import com.weatherxm.data.RewardsObject
import com.weatherxm.data.TransactionsResponse
import com.weatherxm.data.datasource.RewardsDataSource
import java.time.ZoneId

interface RewardsRepository {
    suspend fun getTransactions(
        deviceId: String,
        page: Int?,
        timezone: String? = ZoneId.systemDefault().toString(),
        fromDate: String?,
        toDate: String? = null
    ): Either<Failure, TransactionsResponse>

    suspend fun getRewards(deviceId: String): Either<Failure, Rewards>
    suspend fun getRewardDetails(deviceId: String, txHash: String): Either<Failure, RewardsObject>
}

class RewardsRepositoryImpl(private val dataSource: RewardsDataSource) : RewardsRepository {

    override suspend fun getTransactions(
        deviceId: String,
        page: Int?,
        timezone: String?,
        fromDate: String?,
        toDate: String?
    ): Either<Failure, TransactionsResponse> {
        return dataSource.getTransactions(
            deviceId,
            page,
            timezone = timezone,
            fromDate = fromDate,
            toDate = toDate,
        )
    }

    override suspend fun getRewards(deviceId: String): Either<Failure, Rewards> {
        return dataSource.getRewards(deviceId)
    }

    override suspend fun getRewardDetails(
        deviceId: String,
        txHash: String
    ): Either<Failure, RewardsObject> {
        return dataSource.getRewardDetails(deviceId, txHash)
    }
}