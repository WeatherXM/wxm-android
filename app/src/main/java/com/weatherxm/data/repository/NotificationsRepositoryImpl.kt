package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.datasource.AppConfigDataSource
import com.weatherxm.data.datasource.NotificationsDataSource

class NotificationsRepositoryImpl(
    private val dataSource: NotificationsDataSource,
    private val appConfigDataSource: AppConfigDataSource,
) : NotificationsRepository {
    override suspend fun setFcmToken(): Either<Failure, Unit> {
        return appConfigDataSource.getInstallationId()?.let {
            dataSource.setFcmToken(it, dataSource.getFcmToken())
        } ?: Either.Left(Failure.InstallationIdNotFound)
    }

    override suspend fun deleteFcmToken(): Either<Failure, Unit> {
        return appConfigDataSource.getInstallationId()?.let {
            dataSource.deleteFcmToken(it, dataSource.getFcmToken())
        } ?: Either.Left(Failure.InstallationIdNotFound)
    }
}
