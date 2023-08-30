package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.services.CacheService

class CacheFollowDataSource(private val cacheService: CacheService) : FollowDataSource {
    override suspend fun followStation(deviceId: String): Either<Failure, Unit> {
        val followedStations = getFollowedDevicesIds().toMutableList()
        followedStations.add(deviceId)
        setFollowedDevicesIds(followedStations)
        return Either.Right(Unit)
    }

    override suspend fun unfollowStation(deviceId: String): Either<Failure, Unit> {
        val followedStations = getFollowedDevicesIds().toMutableList()
        followedStations.remove(deviceId)
        setFollowedDevicesIds(followedStations)
        return Either.Right(Unit)
    }

    override suspend fun getFollowedDevicesIds(): List<String> {
        return cacheService.getFollowedDevicesIds()
    }

    override suspend fun setFollowedDevicesIds(ids: List<String>) {
        cacheService.setFollowedDevicesIds(ids)
    }
}
