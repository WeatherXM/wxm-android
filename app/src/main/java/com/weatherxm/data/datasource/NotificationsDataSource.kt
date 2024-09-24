package com.weatherxm.data.datasource

import arrow.core.Either
import com.google.firebase.messaging.FirebaseMessaging
import com.weatherxm.data.models.Failure
import com.weatherxm.data.mapResponse
import com.weatherxm.data.network.ApiService
import kotlinx.coroutines.tasks.await

interface NotificationsDataSource {
    suspend fun setFcmToken(installationId: String, fcmToken: String): Either<Failure, Unit>
    suspend fun getFcmToken(): String
}

class NotificationsDataSourceImpl(
    private val apiService: ApiService,
    private val firebaseMessaging: FirebaseMessaging
) : NotificationsDataSource {
    override suspend fun getFcmToken(): String {
        return firebaseMessaging.token.await()
    }

    override suspend fun setFcmToken(
        installationId: String,
        fcmToken: String
    ): Either<Failure, Unit> {
        return apiService.setFcmToken(installationId, fcmToken).mapResponse()
    }
}
