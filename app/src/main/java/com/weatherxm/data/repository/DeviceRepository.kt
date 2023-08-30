package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.DeviceInfo
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.data.Relation
import com.weatherxm.data.datasource.CacheAddressDataSource
import com.weatherxm.data.datasource.CacheDeviceDataSource
import com.weatherxm.data.datasource.CacheFollowDataSource
import com.weatherxm.data.datasource.NetworkAddressDataSource
import com.weatherxm.data.datasource.NetworkDeviceDataSource
import timber.log.Timber

interface DeviceRepository {
    suspend fun getUserDevices(deviceIds: List<String>? = null): Either<Failure, List<Device>>
    suspend fun getUserDevice(deviceId: String): Either<Failure, Device>
    suspend fun claimDevice(
        serialNumber: String, location: Location, secret: String? = null
    ): Either<Failure, Device>

    suspend fun getDeviceAddress(device: Device): String?
    suspend fun setFriendlyName(deviceId: String, friendlyName: String): Either<Failure, Unit>
    suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit>
    suspend fun removeDevice(serialNumber: String, id: String): Either<Failure, Unit>
    suspend fun getDeviceInfo(deviceId: String): Either<Failure, DeviceInfo>
    suspend fun getUserDevicesIds(): List<String>
}

class DeviceRepositoryImpl(
    private val networkDeviceDataSource: NetworkDeviceDataSource,
    private val cacheDeviceDataSource: CacheDeviceDataSource,
    private val networkAddressDataSource: NetworkAddressDataSource,
    private val cacheAddressDataSource: CacheAddressDataSource,
    private val cacheFollowDataSource: CacheFollowDataSource
) : DeviceRepository {

    override suspend fun getUserDevices(deviceIds: List<String>?): Either<Failure, List<Device>> {
        val ids: String? = if (!deviceIds.isNullOrEmpty()) {
            deviceIds.reduce { temp, vars -> "$temp,$vars" }
        } else {
            null
        }

        return networkDeviceDataSource.getUserDevices(ids).map { devices ->
            devices.onEach {
                it.address = getDeviceAddress(it)
            }.apply {
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
        return networkDeviceDataSource.getUserDevice(deviceId).map { device ->
            device.apply {
                this.address = getDeviceAddress(this)
            }
        }
    }

    override suspend fun claimDevice(
        serialNumber: String, location: Location, secret: String?
    ): Either<Failure, Device> {
        return networkDeviceDataSource.claimDevice(serialNumber, location, secret).onRight {
            val userDevicesIds = cacheDeviceDataSource.getUserDevicesIds().toMutableList()
            userDevicesIds.add(it.id)
            cacheDeviceDataSource.setUserDevicesIds(userDevicesIds)
        }
    }

    override suspend fun getDeviceAddress(device: Device): String? {
        return device.attributes?.hex7?.let { hex ->
            cacheAddressDataSource.getLocationAddress(hex.index, hex.center)
                .onRight { address ->
                    Timber.d("Got location address from cache [$address].")
                }
                .mapLeft {
                    networkAddressDataSource.getLocationAddress(hex.index, hex.center)
                        .onRight { address ->
                            Timber.d("Got location address from network [$address].")
                            Timber.d("Saving location address to cache [$address].")
                            cacheAddressDataSource.setLocationAddress(hex.index, address)
                        }
                }
        }?.getOrNull()
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
            val userDevicesIds = cacheDeviceDataSource.getUserDevicesIds().toMutableList()
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
}
