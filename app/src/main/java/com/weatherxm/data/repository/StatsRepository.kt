package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.NetworkStatsResponse

interface StatsRepository {
    suspend fun getNetworkStats(): Either<Failure, NetworkStatsResponse>
}
