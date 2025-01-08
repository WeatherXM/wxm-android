package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.datasource.DevicePhotoDataSource
import com.weatherxm.data.models.Failure

interface DevicePhotoRepository {
    suspend fun getDevicePhotos(deviceId: String): Either<Failure, List<String>>
    suspend fun deleteDevicePhoto(deviceId: String, photoPath: String): Either<Failure, Unit>
    fun getAcceptedTerms(): Boolean
    fun setAcceptedTerms()
}

class DevicePhotoRepositoryImpl(
    private val datasource: DevicePhotoDataSource
) : DevicePhotoRepository {
    override suspend fun getDevicePhotos(deviceId: String): Either<Failure, List<String>> {
        return datasource.getDevicePhotos(deviceId)
    }

    override suspend fun deleteDevicePhoto(
        deviceId: String,
        photoPath: String
    ): Either<Failure, Unit> {
        /**
         * photoPath = https://weatherxm.com/my-weather-station/photo1.jpg
         * will become
         * photoName = photo1
         */
        val photoName = photoPath.substringAfterLast('/').substringBeforeLast('.')
        return datasource.deleteDevicePhoto(deviceId, photoName)
    }

    override fun getAcceptedTerms(): Boolean {
        return datasource.getAcceptedTerms()
    }

    override fun setAcceptedTerms() {
        datasource.setAcceptedTerms()
    }
}
