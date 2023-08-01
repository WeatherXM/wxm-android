package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.map
import com.weatherxm.data.network.ApiService

class NetworkFollowDataSource(private val apiService: ApiService) : FollowDataSource {
    override suspend fun followStation(deviceId: String): Either<Failure, Unit> {
        return apiService.followStation(deviceId).map()
    }

    override suspend fun unfollowStation(deviceId: String): Either<Failure, Unit> {
        return apiService.unfollowStation(deviceId).map()
    }

    override suspend fun getFollowedStationIds(): Either<Failure, List<String>> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun setFollowedStationIds(ids: List<String>) {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }
}
