package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.FollowRepository

class FollowUseCaseImpl(private val repository: FollowRepository) : FollowUseCase {
    override suspend fun followStation(deviceId: String): Either<Failure, Unit> {
        return repository.followStation(deviceId)
    }

    override suspend fun unfollowStation(deviceId: String): Either<Failure, Unit> {
        return repository.unfollowStation(deviceId)
    }
}
