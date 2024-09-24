package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.NetworkStatsResponse
import com.weatherxm.data.mapResponse
import com.weatherxm.data.network.ApiService

interface StatsDataSource {
    suspend fun getNetworkStats(): Either<Failure, NetworkStatsResponse>
}

class StatsDataSourceImpl(private val apiService: ApiService) : StatsDataSource {
    override suspend fun getNetworkStats(): Either<Failure, NetworkStatsResponse> {
        return apiService.getNetworkStats().mapResponse()
    }
}
