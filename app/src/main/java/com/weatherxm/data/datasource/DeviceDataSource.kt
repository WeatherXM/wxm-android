package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.data.map
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.ClaimDeviceBody
import com.weatherxm.data.network.FriendlyNameBody

interface DeviceDataSource {
    suspend fun getUserDevices(): Either<Failure, List<Device>>
    suspend fun getUserDevice(deviceId: String): Either<Failure, Device>
    suspend fun claimDevice(serialNumber: String, location: Location): Either<Failure, Device>
    suspend fun setFriendlyName(deviceId: String, friendlyName: String): Either<Failure, Unit>
    suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit>
}

class DeviceDataSourceImpl(private val apiService: ApiService) : DeviceDataSource {

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

    override suspend fun setFriendlyName(
        deviceId: String,
        friendlyName: String
    ): Either<Failure, Unit> {
        return apiService.setFriendlyName(deviceId, FriendlyNameBody(friendlyName)).map()
    }

    override suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit> {
        return apiService.clearFriendlyName(deviceId).map()
    }
}
