package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.datasource.CacheFollowDataSource
import com.weatherxm.data.datasource.NetworkFollowDataSource
import com.weatherxm.data.models.Failure

interface FollowRepository {
    suspend fun followStation(deviceId: String): Either<Failure, Unit>
    suspend fun unfollowStation(deviceId: String): Either<Failure, Unit>
    suspend fun getFollowedDevicesIds(): List<String>
}

class FollowRepositoryImpl(
    private val networkDataSource: NetworkFollowDataSource,
    private val cacheFollowDataSource: CacheFollowDataSource
) : FollowRepository {
    override suspend fun followStation(deviceId: String): Either<Failure, Unit> {
        return networkDataSource.followStation(deviceId).onRight {
            cacheFollowDataSource.followStation(deviceId)
        }
    }

    override suspend fun unfollowStation(deviceId: String): Either<Failure, Unit> {
        return networkDataSource.unfollowStation(deviceId).onLeft {
            // Unfollowing failed on the API, set as followed again in our cache
            cacheFollowDataSource.followStation(deviceId)
        }.onRight {
            cacheFollowDataSource.unfollowStation(deviceId)
        }
    }

    override suspend fun getFollowedDevicesIds(): List<String> {
        return cacheFollowDataSource.getFollowedDevicesIds()
    }
}
