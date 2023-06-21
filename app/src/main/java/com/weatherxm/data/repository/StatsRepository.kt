package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.NetworkStatsResponse

interface StatsRepository {
    suspend fun getNetworkStats(): Either<Failure, NetworkStatsResponse>
}
