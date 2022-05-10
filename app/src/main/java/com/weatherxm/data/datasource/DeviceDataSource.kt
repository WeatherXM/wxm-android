package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.data.map
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.ClaimDeviceBody

interface DeviceDataSource {
    suspend fun getPublicDevices(forceRefresh: Boolean): Either<Failure, List<Device>>
    suspend fun getUserDevices(): Either<Failure, List<Device>>
    suspend fun getUserDevice(deviceId: String): Either<Failure, Device>
    suspend fun claimDevice(serialNumber: String, location: Location): Either<Failure, Device>
}

class DeviceDataSourceImpl(
    private val apiService: ApiService
) : DeviceDataSource {
    private lateinit var publicDevices: List<Device>

    override suspend fun getPublicDevices(forceRefresh: Boolean): Either<Failure, List<Device>> {
        return if (this::publicDevices.isInitialized && !forceRefresh) {
            Either.Right(publicDevices)
        } else {
            apiService.getPublicDevices().map().tap {
                publicDevices = it
            }
        }
    }

    override suspend fun getUserDevices(): Either<Failure, List<Device>> {
        return apiService.getUserDevices().map()
    }

    override suspend fun getUserDevice(deviceId: String): Either<Failure, Device> {
        return apiService.getUserDevice(deviceId).map()
    }

    override suspend fun claimDevice(
        serialNumber: String,
        location: Location
    ): Either<Failure, Device> {
        return apiService.claimDevice(ClaimDeviceBody(serialNumber, location)).map()
    }
}
