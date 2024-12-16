package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.datasource.DevicePhotoDataSource
import com.weatherxm.data.models.DevicePhoto
import com.weatherxm.data.models.Failure

interface DevicePhotoRepository {
    suspend fun getDevicePhotos(deviceId: String): Either<Failure, List<DevicePhoto>>
    fun getAcceptedTerms(): Boolean
    fun setAcceptedTerms()
}

class DevicePhotoRepositoryImpl(
    private val datasource: DevicePhotoDataSource
) : DevicePhotoRepository {
    override suspend fun getDevicePhotos(deviceId: String): Either<Failure, List<DevicePhoto>> {
        return datasource.getDevicePhotos(deviceId)
    }

    override fun getAcceptedTerms(): Boolean {
        return datasource.getAcceptedTerms()
    }

    override fun setAcceptedTerms() {
        datasource.setAcceptedTerms()
    }
}
