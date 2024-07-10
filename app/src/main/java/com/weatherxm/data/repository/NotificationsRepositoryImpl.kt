package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.datasource.AppConfigDataSource
import com.weatherxm.data.datasource.CacheAuthDataSource
import com.weatherxm.data.datasource.NotificationsDataSource

class NotificationsRepositoryImpl(
    private val dataSource: NotificationsDataSource,
    private val authDataSource: CacheAuthDataSource,
    private val appConfigDataSource: AppConfigDataSource,
) : NotificationsRepository {
    override suspend fun onRefreshedToken(fcmToken: String) {
        /**
         * Only set the new FCM token if the user is already logged in.
         * Otherwise this set will take place on login.
         */
        if (authDataSource.getAuthToken()
                .isRight { it.isAccessTokenValid() || it.isRefreshTokenValid() }
        ) {
            setFcmToken(fcmToken)
        }
    }

    override suspend fun setFcmToken(fcmToken: String?): Either<Failure, Unit> {
        return appConfigDataSource.getInstallationId()?.let {
            dataSource.setFcmToken(it, fcmToken ?: dataSource.getFcmToken())
        } ?: Either.Left(Failure.InstallationIdNotFound)
    }

    override suspend fun deleteFcmToken(): Either<Failure, Unit> {
        return appConfigDataSource.getInstallationId()?.let {
            dataSource.deleteFcmToken(it, dataSource.getFcmToken())
        } ?: Either.Left(Failure.InstallationIdNotFound)
    }
}
