package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure

interface FollowDataSource {
    suspend fun followStation(deviceId: String): Either<Failure, Unit>
    suspend fun unfollowStation(deviceId: String): Either<Failure, Unit>
    suspend fun getFollowedStationIds(): Either<Failure, List<String>>
    suspend fun setFollowedStationIds(ids: List<String>)
}
