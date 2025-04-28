package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.models.Device
import com.weatherxm.data.models.DeviceInfo
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.data.services.CacheService

class CacheDeviceDataSource(private val cacheService: CacheService) : DeviceDataSource {
    override suspend fun getUserDevices(): Either<Failure, List<Device>> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun getUserDevice(deviceId: String): Either<Failure, Device> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun claimDevice(
        serialNumber: String,
        location: Location,
        secret: String?,
        numOfRetries: Int
    ): Either<Failure, Device> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun setFriendlyName(
        deviceId: String,
        friendlyName: String
    ): Either<Failure, Unit> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun removeDevice(serialNumber: String): Either<Failure, Unit> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun getDeviceInfo(deviceId: String): Either<Failure, DeviceInfo> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun getUserDevicesFromCache(): List<Device> {
        return cacheService.getUserDevices()
    }

    override suspend fun setUserDevices(devices: List<Device>) {
        cacheService.setUserDevices(devices)
        devices.groupBy { it.bundle }.forEach {
            it.key?.name?.let { name ->
                cacheService.setUserDevicesOfBundle(
                    CacheService.getUserDeviceOwnFormattedKey(name),
                    it.value.size
                )
            }
        }
    }

    override suspend fun setLocation(
        deviceId: String,
        lat: Double,
        lon: Double
    ): Either<Failure, Device> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }
}
