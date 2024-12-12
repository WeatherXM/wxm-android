package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.mapResponse
import com.weatherxm.data.models.DevicePhoto
import com.weatherxm.data.models.Failure
import com.weatherxm.data.network.ApiService

interface DevicePhotoDataSource {
    suspend fun getDevicePhotos(deviceId: String): Either<Failure, List<DevicePhoto>>
}

class DevicePhotoDataSourceImpl(private val apiService: ApiService) : DevicePhotoDataSource {
    override suspend fun getDevicePhotos(deviceId: String): Either<Failure, List<DevicePhoto>> {
        return apiService.getDevicePhotos(deviceId).mapResponse()
    }
}
