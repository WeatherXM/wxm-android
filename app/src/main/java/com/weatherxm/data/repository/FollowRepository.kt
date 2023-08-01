package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.datasource.CacheFollowDataSource
import com.weatherxm.data.datasource.NetworkFollowDataSource

interface FollowRepository {
    suspend fun followStation(deviceId: String): Either<Failure, Unit>
    suspend fun unfollowStation(deviceId: String): Either<Failure, Unit>
    suspend fun getFollowedStationIds(): Either<Failure, List<String>>
    suspend fun setFollowedStationIds(ids: List<String>)
}

class FollowRepositoryImpl(
    private val networkDataSource: NetworkFollowDataSource,
    private val cacheDataSource: CacheFollowDataSource
) : FollowRepository {
    override suspend fun followStation(deviceId: String): Either<Failure, Unit> {
        cacheDataSource.followStation(deviceId)
        return networkDataSource.followStation(deviceId).onLeft {
            // Something went wrong with the API, set as unfollowed again in cache
            cacheDataSource.unfollowStation(deviceId)
        }
    }

    override suspend fun unfollowStation(deviceId: String): Either<Failure, Unit> {
        cacheDataSource.unfollowStation(deviceId)
        return networkDataSource.followStation(deviceId).onLeft {
            // Something went wrong with the API, set as followed again in cache
            cacheDataSource.followStation(deviceId)
        }
    }

    override suspend fun getFollowedStationIds(): Either<Failure, List<String>> {
        return cacheDataSource.getFollowedStationIds()
    }

    override suspend fun setFollowedStationIds(ids: List<String>) {
        cacheDataSource.setFollowedStationIds(ids)
    }
}
