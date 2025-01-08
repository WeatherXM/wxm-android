package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.repository.DevicePhotoRepository

interface DevicePhotoUseCase {
    suspend fun getDevicePhotos(deviceId: String): Either<Failure, List<String>>
    suspend fun deleteDevicePhoto(deviceId: String, photoPath: String): Either<Failure, Unit>
    fun getAcceptedTerms(): Boolean
    fun setAcceptedTerms()
}

class DevicePhotoUseCaseImpl(
    private val repository: DevicePhotoRepository
) : DevicePhotoUseCase {
    override suspend fun getDevicePhotos(deviceId: String): Either<Failure, List<String>> {
        return repository.getDevicePhotos(deviceId)
    }

    override suspend fun deleteDevicePhoto(
        deviceId: String,
        photoPath: String
    ): Either<Failure, Unit> {
        return repository.deleteDevicePhoto(deviceId, photoPath)
    }

    override fun getAcceptedTerms(): Boolean {
        return repository.getAcceptedTerms()
    }

    override fun setAcceptedTerms() {
        repository.setAcceptedTerms()
    }
}
