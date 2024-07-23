package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.leftToFailure
import com.weatherxm.data.network.ApiService

class NetworkFollowDataSource(private val apiService: ApiService) : FollowDataSource {
    override suspend fun followStation(deviceId: String): Either<Failure, Unit> {
        return apiService.followStation(deviceId).leftToFailure()
    }

    override suspend fun unfollowStation(deviceId: String): Either<Failure, Unit> {
        return apiService.unfollowStation(deviceId).leftToFailure()
    }

    override suspend fun getFollowedDevicesIds(): List<String> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun setFollowedDevicesIds(ids: List<String>) {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }
}
