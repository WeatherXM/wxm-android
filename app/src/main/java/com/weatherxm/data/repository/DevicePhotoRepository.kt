package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.datasource.DevicePhotoDataSource
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.PhotoPresignedMetadata
import java.util.UUID

interface DevicePhotoRepository {
    suspend fun getDevicePhotos(deviceId: String): Either<Failure, List<String>>
    suspend fun deleteDevicePhoto(deviceId: String, photoPath: String): Either<Failure, Unit>
    suspend fun getPhotosMetadataForUpload(
        deviceId: String,
        photoPaths: List<String>
    ): Either<Failure, List<PhotoPresignedMetadata>>

    fun getAcceptedTerms(): Boolean
    fun setAcceptedTerms()
    fun getDevicePhotoUploadingIds(deviceId: String): List<String>
    fun addDevicePhotoUploadingId(deviceId: String, uploadingId: String)
    fun removeDevicePhotoUploadingId(deviceId: String, uploadingId: String)
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
         * photoName = photo1.jpg
         */
        val photoName = photoPath.substringAfterLast('/')
        return datasource.deleteDevicePhoto(deviceId, photoName)
    }

    override suspend fun getPhotosMetadataForUpload(
        deviceId: String,
        photoPaths: List<String>
    ): Either<Failure, List<PhotoPresignedMetadata>> {
        val photoNames = photoPaths.map {
            val uuid = UUID.nameUUIDFromBytes(it.substringAfterLast('/').toByteArray()).toString()
            "$uuid.jpg"
        }
        return datasource.getPhotosMetadataForUpload(deviceId, photoNames)
    }

    override fun getAcceptedTerms(): Boolean {
        return datasource.getAcceptedTerms()
    }

    override fun setAcceptedTerms() {
        datasource.setAcceptedTerms()
    }

    override fun getDevicePhotoUploadingIds(deviceId: String): List<String> {
        return datasource.getDevicePhotoUploadingIds(deviceId)
    }

    override fun addDevicePhotoUploadingId(deviceId: String, uploadingId: String) {
        datasource.addDevicePhotoUploadingId(deviceId, uploadingId)
    }

    override fun removeDevicePhotoUploadingId(deviceId: String, uploadingId: String) {
        datasource.removeDevicePhotoUploadingId(deviceId, uploadingId)
    }
}
