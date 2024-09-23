package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.NetworkStatsResponse
import com.weatherxm.data.datasource.StatsDataSource

class StatsRepositoryImpl(private val dataSource: StatsDataSource) : StatsRepository {
    override suspend fun getNetworkStats(): Either<Failure, NetworkStatsResponse> {
        return dataSource.getNetworkStats()
    }
}
