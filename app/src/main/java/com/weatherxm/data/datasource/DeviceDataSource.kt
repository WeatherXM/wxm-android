package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.DeviceInfo
import com.weatherxm.data.Failure
import com.weatherxm.data.Location

interface DeviceDataSource {
    suspend fun getUserDevices(): Either<Failure, List<Device>>
    suspend fun getUserDevice(deviceId: String): Either<Failure, Device>
    suspend fun claimDevice(
        serialNumber: String,
        location: Location,
        secret: String? = null,
        numOfRetries: Int = 0
    ): Either<Failure, Device>

    suspend fun setFriendlyName(deviceId: String, friendlyName: String): Either<Failure, Unit>
    suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit>
    suspend fun removeDevice(serialNumber: String): Either<Failure, Unit>
    suspend fun getDeviceInfo(deviceId: String): Either<Failure, DeviceInfo>
    suspend fun getUserDevicesIds(): List<String>
    suspend fun setUserDevicesIds(ids: List<String>)
    suspend fun setLocation(
        deviceId: String,
        lat: Double,
        lon: Double
    ): Either<Failure, Device>
}
