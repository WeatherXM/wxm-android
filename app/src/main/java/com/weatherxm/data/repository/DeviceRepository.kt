package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.DeviceInfo
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.data.datasource.CacheAddressDataSource
import com.weatherxm.data.datasource.DeviceDataSource
import com.weatherxm.data.datasource.NetworkAddressDataSource
import com.weatherxm.data.datasource.UserActionDataSource
import timber.log.Timber
import java.util.Date

interface DeviceRepository {
    suspend fun getUserDevices(deviceIds: List<String>? = null): Either<Failure, List<Device>>
    suspend fun getUserDevice(deviceId: String): Either<Failure, Device>
    suspend fun claimDevice(
        serialNumber: String, location: Location, secret: String? = null
    ): Either<Failure, Device>

    suspend fun getDeviceAddress(device: Device): String?
    suspend fun setFriendlyName(deviceId: String, friendlyName: String): Either<Failure, Unit>
    suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit>
    suspend fun getLastFriendlyNameChanged(deviceId: String): Long
    suspend fun removeDevice(serialNumber: String): Either<Failure, Unit>
    suspend fun getDeviceInfo(deviceId: String): Either<Failure, DeviceInfo>
}

class DeviceRepositoryImpl(
    private val deviceDataSource: DeviceDataSource,
    private val networkAddressDataSource: NetworkAddressDataSource,
    private val cacheAddressDataSource: CacheAddressDataSource,
    private val userActionDataSource: UserActionDataSource
) : DeviceRepository {

    override suspend fun getUserDevices(deviceIds: List<String>?): Either<Failure, List<Device>> {
        val ids: String? = if (!deviceIds.isNullOrEmpty()) {
            deviceIds.reduce { temp, vars -> "$temp,$vars" }
        } else {
            null
        }

        return deviceDataSource.getUserDevices(ids).map { devices ->
            devices.onEach {
                it.address = getDeviceAddress(it)
            }
        }
    }

    override suspend fun getUserDevice(deviceId: String): Either<Failure, Device> {
        return deviceDataSource.getUserDevice(deviceId).map { device ->
            device.apply {
                this.address = getDeviceAddress(this)
            }
        }
    }

    override suspend fun claimDevice(
        serialNumber: String, location: Location, secret: String?
    ): Either<Failure, Device> {
        return deviceDataSource.claimDevice(serialNumber, location, secret)
    }

    override suspend fun getDeviceAddress(device: Device): String? {
        var hexAddress: String? = null

        device.attributes?.hex7?.let { hex ->
            cacheAddressDataSource.getLocationAddress(hex.index, hex.center).onRight { address ->
                Timber.d("Got location address from database [$address].")
                hexAddress = address
            }.mapLeft {
                networkAddressDataSource.getLocationAddress(hex.index, hex.center)
                    .onRight { address ->
                        Timber.d("Got location address from network [$it].")
                        hexAddress = address
                        address?.let {
                            cacheAddressDataSource.setLocationAddress(hex.index, it)
                        }
                    }
            }
        }

        return hexAddress
    }

    override suspend fun setFriendlyName(
        deviceId: String, friendlyName: String
    ): Either<Failure, Unit> {
        return deviceDataSource.setFriendlyName(deviceId, friendlyName).onRight {
            userActionDataSource.setLastFriendlyNameChanged(deviceId, Date().time)
        }
    }

    override suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit> {
        return deviceDataSource.clearFriendlyName(deviceId).onRight {
            userActionDataSource.setLastFriendlyNameChanged(deviceId, Date().time)
        }
    }

    override suspend fun getLastFriendlyNameChanged(deviceId: String): Long {
        return userActionDataSource.getLastFriendlyNameChanged(deviceId)
    }

    override suspend fun removeDevice(serialNumber: String): Either<Failure, Unit> {
        return deviceDataSource.removeDevice(serialNumber)
    }

    override suspend fun getDeviceInfo(deviceId: String): Either<Failure, DeviceInfo> {
        return deviceDataSource.getDeviceInfo(deviceId)
    }
}
