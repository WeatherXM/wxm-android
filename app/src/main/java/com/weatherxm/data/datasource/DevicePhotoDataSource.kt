package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.mapResponse
import com.weatherxm.data.models.DevicePhoto
import com.weatherxm.data.models.Failure
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.services.CacheService

interface DevicePhotoDataSource {
    suspend fun getDevicePhotos(deviceId: String): Either<Failure, List<DevicePhoto>>
    fun getAcceptedTerms(): Boolean
    fun setAcceptedTerms()
}

class DevicePhotoDataSourceImpl(
    private val apiService: ApiService,
    private val cacheService: CacheService
) : DevicePhotoDataSource {
    override suspend fun getDevicePhotos(deviceId: String): Either<Failure, List<DevicePhoto>> {
        return apiService.getDevicePhotos(deviceId).mapResponse()
    }

    override fun getAcceptedTerms(): Boolean {
        return cacheService.getPhotoVerificationAcceptedTerms()
    }

    override fun setAcceptedTerms() {
        cacheService.setPhotoVerificationAcceptedTerms()
    }
}
