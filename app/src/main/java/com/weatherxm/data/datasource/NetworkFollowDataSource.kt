package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.mapResponse
import com.weatherxm.data.network.ApiService

class NetworkFollowDataSource(private val apiService: ApiService) : FollowDataSource {
    override suspend fun followStation(deviceId: String): Either<Failure, Unit> {
        return apiService.followStation(deviceId).mapResponse()
    }

    override suspend fun unfollowStation(deviceId: String): Either<Failure, Unit> {
        return apiService.unfollowStation(deviceId).mapResponse()
    }

    override suspend fun getFollowedDevicesIds(): List<String> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun setFollowedDevicesIds(ids: List<String>) {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }
}
