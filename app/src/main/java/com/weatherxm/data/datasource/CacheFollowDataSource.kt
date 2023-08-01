package com.weatherxm.data.datasource

import arrow.core.Either
import arrow.core.getOrElse
import com.weatherxm.data.Failure
import com.weatherxm.data.services.CacheService

class CacheFollowDataSource(private val cacheService: CacheService) : FollowDataSource {
    override suspend fun followStation(deviceId: String): Either<Failure, Unit> {
        val followedStations = getFollowedStationIds().getOrElse { mutableListOf() }.toMutableList()
        followedStations.add(deviceId)
        setFollowedStationIds(followedStations)
        return Either.Right(Unit)
    }

    override suspend fun unfollowStation(deviceId: String): Either<Failure, Unit> {
        val followedStations = getFollowedStationIds().getOrElse { mutableListOf() }.toMutableList()
        followedStations.remove(deviceId)
        setFollowedStationIds(followedStations)
        return Either.Right(Unit)
    }

    override suspend fun getFollowedStationIds(): Either<Failure, List<String>> {
        return cacheService.getFollowedStationIds()
    }

    override suspend fun setFollowedStationIds(ids: List<String>) {
        cacheService.setFollowedStationIds(ids)
    }
}
