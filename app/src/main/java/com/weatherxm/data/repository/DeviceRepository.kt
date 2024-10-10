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
}

class DeviceRepositoryImpl(
    private val networkDeviceDataSource: NetworkDeviceDataSource,
    private val cacheDeviceDataSource: CacheDeviceDataSource,
    private val cacheFollowDataSource: CacheFollowDataSource
) : DeviceRepository {

    override suspend fun getUserDevices(): Either<Failure, List<Device>> {
        return networkDeviceDataSource.getUserDevices().map { devices ->
            devices.apply {
                cacheDeviceDataSource.setUserDevicesIds(this.filter {
                    it.relation == Relation.owned
                }.map {
                    it.id
                })
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
            val userDevicesIds = getUserDevicesIds().toMutableList()
            userDevicesIds.add(it.id)
            cacheDeviceDataSource.setUserDevicesIds(userDevicesIds)
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
            val userDevicesIds = getUserDevicesIds().toMutableList()
            userDevicesIds.remove(id)
            cacheDeviceDataSource.setUserDevicesIds(userDevicesIds)
        }
    }

    override suspend fun getDeviceInfo(deviceId: String): Either<Failure, DeviceInfo> {
        return networkDeviceDataSource.getDeviceInfo(deviceId)
    }

    override suspend fun getUserDevicesIds(): List<String> {
        return cacheDeviceDataSource.getUserDevicesIds()
    }

    override suspend fun setLocation(
        deviceId: String,
        lat: Double,
        lon: Double
    ): Either<Failure, Device> {
        return networkDeviceDataSource.setLocation(deviceId, lat, lon)
    }
}
