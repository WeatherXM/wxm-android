package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.Failure

interface FollowUseCase {
    suspend fun followStation(deviceId: String): Either<Failure, Unit>
    suspend fun unfollowStation(deviceId: String): Either<Failure, Unit>
}
