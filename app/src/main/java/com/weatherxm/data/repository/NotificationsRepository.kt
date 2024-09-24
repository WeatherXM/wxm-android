package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.models.Failure

interface NotificationsRepository {
    suspend fun setFcmToken(fcmToken: String? = null): Either<Failure, Unit>
}
