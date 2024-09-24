package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.models.Failure

interface FollowDataSource {
    suspend fun followStation(deviceId: String): Either<Failure, Unit>
    suspend fun unfollowStation(deviceId: String): Either<Failure, Unit>
    suspend fun getFollowedDevicesIds(): List<String>
    suspend fun setFollowedDevicesIds(ids: List<String>)
}
