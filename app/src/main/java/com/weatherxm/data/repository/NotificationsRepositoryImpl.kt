package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.datasource.AppConfigDataSource
import com.weatherxm.data.datasource.NotificationsDataSource

class NotificationsRepositoryImpl(
    private val dataSource: NotificationsDataSource,
    private val appConfigDataSource: AppConfigDataSource,
) : NotificationsRepository {
    override suspend fun setFcmToken(fcmToken: String?): Either<Failure, Unit> {
        return appConfigDataSource.getInstallationId()?.let {
            dataSource.setFcmToken(it, fcmToken ?: dataSource.getFcmToken())
        } ?: Either.Left(Failure.InstallationIdNotFound)
    }
}
