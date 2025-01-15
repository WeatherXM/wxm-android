package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.mapResponse
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.PhotoPresignedMetadata
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.PhotoNamesBody
import com.weatherxm.data.services.CacheService
import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest

interface DevicePhotoDataSource {
    suspend fun getDevicePhotos(deviceId: String): Either<Failure, List<String>>
    suspend fun deleteDevicePhoto(deviceId: String, photoName: String): Either<Failure, Unit>
    suspend fun getPhotosMetadataForUpload(
        deviceId: String,
        photoNames: List<String>
    ): Either<Failure, List<PhotoPresignedMetadata>>

    fun getAcceptedTerms(): Boolean
    fun setAcceptedTerms()
    fun getDevicePhotoUploadIds(deviceId: String): List<String>
    fun addDevicePhotoUploadIdAndRequest(
        deviceId: String,
        uploadId: String,
        request: MultipartUploadRequest
    )

    fun getUploadIdRequest(uploadId: String): MultipartUploadRequest?
}

class DevicePhotoDataSourceImpl(
    private val apiService: ApiService,
    private val cacheService: CacheService
) : DevicePhotoDataSource {
    override suspend fun getDevicePhotos(deviceId: String): Either<Failure, List<String>> {
        return apiService.getDevicePhotos(deviceId).mapResponse()
    }

    override suspend fun deleteDevicePhoto(
        deviceId: String,
        photoName: String
    ): Either<Failure, Unit> {
        return apiService.deleteDevicePhoto(deviceId, photoName).mapResponse()
    }

    override suspend fun getPhotosMetadataForUpload(
        deviceId: String,
        photoNames: List<String>
    ): Either<Failure, List<PhotoPresignedMetadata>> {
        return apiService.getPhotosMetadataForUpload(deviceId, PhotoNamesBody(photoNames))
            .mapResponse()
    }

    override fun getAcceptedTerms(): Boolean {
        return cacheService.getPhotoVerificationAcceptedTerms()
    }

    override fun setAcceptedTerms() {
        cacheService.setPhotoVerificationAcceptedTerms()
    }

    override fun getDevicePhotoUploadIds(deviceId: String): List<String> {
        return cacheService.getDevicePhotoUploadIds(deviceId)
    }

    override fun addDevicePhotoUploadIdAndRequest(
        deviceId: String,
        uploadId: String,
        request: MultipartUploadRequest
    ) {
        cacheService.setUploadIdRequest(uploadId, request)
        cacheService.addDevicePhotoUploadId(deviceId, uploadId)
    }

    override fun getUploadIdRequest(uploadId: String): MultipartUploadRequest? {
        return cacheService.getUploadIdRequest(uploadId)
    }
}
