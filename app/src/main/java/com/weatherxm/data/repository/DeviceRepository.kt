package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.datasource.CacheDeviceDataSource
import com.weatherxm.data.datasource.CacheFollowDataSource
import com.weatherxm.data.datasource.NetworkDeviceDataSource
import com.weatherxm.data.models.Device
import com.weatherxm.data.models.DeviceInfo
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.Relation

interface DeviceRepository {
    suspend fun getUserDevices(): Either<Failure, List<Device>>
    suspend fun getUserDevice(deviceId: String): Either<Failure, Device>
    suspend fun claimDevice(
        serialNumber: String, location: Location, secret: String? = null
    ): Either<Failure, Device>

    suspend fun setFriendlyName(deviceId: String, friendlyName: String): Either<Failure, Unit>
    suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit>
    suspend fun removeDevice(serialNumber: String, id: String): Either<Failure, Unit>
    suspend fun getDeviceInfo(deviceId: String): Either<Failure, DeviceInfo>
    suspend fun getUserDevicesIds(): List<String>
    suspend fun setLocation(deviceId: String, lat: Double, lon: Double): Either<Failure, Device>
    suspend fun getDeviceHealthCheck(deviceName: String): String?
}

class DeviceRepositoryImpl(
    private val networkDeviceDataSource: NetworkDeviceDataSource,
    private val cacheDeviceDataSource: CacheDeviceDataSource,
    private val cacheFollowDataSource: CacheFollowDataSource
) : DeviceRepository {

    override suspend fun getUserDevices(): Either<Failure, List<Device>> {
        return networkDeviceDataSource.getUserDevices().map { devices ->
            devices.apply {
                cacheDeviceDataSource.setUserDevices(this.filter { it.relation == Relation.owned })
                cacheFollowDataSource.setFollowedDevicesIds(this.filter {
                    it.relation == Relation.followed
                }.map {
                    it.id
                })
            }
        }
    }

    override suspend fun getUserDevice(deviceId: String): Either<Failure, Device> {
        return networkDeviceDataSource.getUserDevice(deviceId)
    }

    override suspend fun claimDevice(
        serialNumber: String, location: Location, secret: String?
    ): Either<Failure, Device> {
        return networkDeviceDataSource.claimDevice(serialNumber, location, secret).onRight {
            val userDevices = cacheDeviceDataSource.getUserDevicesFromCache().toMutableList()
            userDevices.add(it)
            cacheDeviceDataSource.setUserDevices(userDevices)
        }
    }

    override suspend fun setFriendlyName(
        deviceId: String, friendlyName: String
    ): Either<Failure, Unit> {
        return networkDeviceDataSource.setFriendlyName(deviceId, friendlyName)
    }

    override suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit> {
        return networkDeviceDataSource.clearFriendlyName(deviceId)
    }

    override suspend fun removeDevice(serialNumber: String, id: String): Either<Failure, Unit> {
        return networkDeviceDataSource.removeDevice(serialNumber).onRight {
            val userDevices = cacheDeviceDataSource.getUserDevicesFromCache().toMutableList()
            userDevices.removeIf { it.id == id }
            cacheDeviceDataSource.setUserDevices(userDevices)
        }
    }

    override suspend fun getDeviceInfo(deviceId: String): Either<Failure, DeviceInfo> {
        return networkDeviceDataSource.getDeviceInfo(deviceId)
    }

    override suspend fun getUserDevicesIds(): List<String> {
        return cacheDeviceDataSource.getUserDevicesFromCache().map { it.id }
    }

    override suspend fun setLocation(
        deviceId: String,
        lat: Double,
        lon: Double
    ): Either<Failure, Device> {
        return networkDeviceDataSource.setLocation(deviceId, lat, lon)
    }

    override suspend fun getDeviceHealthCheck(deviceName: String): String? {
        return networkDeviceDataSource.getDeviceHealthCheck(deviceName)
    }
}
