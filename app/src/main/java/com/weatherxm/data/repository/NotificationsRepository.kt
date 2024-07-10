package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure

interface NotificationsRepository {
    suspend fun setFcmToken(): Either<Failure, Unit>
    suspend fun deleteFcmToken(): Either<Failure, Unit>
}
