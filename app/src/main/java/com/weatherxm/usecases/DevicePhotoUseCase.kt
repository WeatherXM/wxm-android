package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.PhotoPresignedMetadata
import com.weatherxm.data.repository.DevicePhotoRepository

interface DevicePhotoUseCase {
    suspend fun getDevicePhotos(deviceId: String): Either<Failure, List<String>>
    suspend fun deleteDevicePhoto(deviceId: String, photoPath: String): Either<Failure, Unit>
    suspend fun getPhotosMetadataForUpload(
        deviceId: String,
        photoPaths: List<String>
    ): Either<Failure, List<PhotoPresignedMetadata>>

    fun getAcceptedTerms(): Boolean
    fun setAcceptedTerms()
    fun getDevicePhotoUploadIds(deviceId: String): List<String>
    fun removeDevicePhotoUploadId(deviceId: String, uploadId: String)
    fun retryUpload(deviceId: String)
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

    override suspend fun getPhotosMetadataForUpload(
        deviceId: String,
        photoPaths: List<String>
    ): Either<Failure, List<PhotoPresignedMetadata>> {
        return repository.getPhotosMetadataForUpload(deviceId, photoPaths)
    }

    override fun getAcceptedTerms(): Boolean {
        return repository.getAcceptedTerms()
    }

    override fun setAcceptedTerms() {
        repository.setAcceptedTerms()
    }

    override fun getDevicePhotoUploadIds(deviceId: String): List<String> {
        return repository.getDevicePhotoUploadIds(deviceId)
    }

    override fun removeDevicePhotoUploadId(deviceId: String, uploadId: String) {
        repository.removeDevicePhotoUploadId(deviceId, uploadId)
    }

    override fun retryUpload(deviceId: String) {
        getDevicePhotoUploadIds(deviceId).forEach {
            // TODO: STOPSHIP: This crashes. https://github.com/gotev/android-upload-service/issues/672 
            val request = repository.getUploadIdRequest(it)
            request?.setUploadID(it)
            request?.startUpload()
        }
    }
}
