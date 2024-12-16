package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.DevicePhoto
import com.weatherxm.data.models.Failure
import com.weatherxm.data.repository.DevicePhotoRepository

interface DevicePhotoUseCase {
    suspend fun getDevicePhotos(deviceId: String): Either<Failure, List<DevicePhoto>>
}

class DevicePhotoUseCaseImpl(
    private val repository: DevicePhotoRepository
) : DevicePhotoUseCase {
    override suspend fun getDevicePhotos(deviceId: String): Either<Failure, List<DevicePhoto>> {
        return repository.getDevicePhotos(deviceId)
    }
}
