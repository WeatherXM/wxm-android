package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.data.datasource.DeviceDataSource
import com.weatherxm.data.datasource.NetworkAddressDataSource
import com.weatherxm.data.datasource.StorageAddressDataSource
import com.weatherxm.data.datasource.UserActionDataSource
import timber.log.Timber
import java.util.Date

interface DeviceRepository {
    suspend fun getUserDevices(): Either<Failure, List<Device>>
    suspend fun getUserDevice(deviceId: String): Either<Failure, Device>
    suspend fun claimDevice(
        serialNumber: String,
        location: Location,
        secret: String? = null
    ): Either<Failure, Device>

    suspend fun getDeviceAddress(device: Device): String?
    suspend fun setFriendlyName(deviceId: String, friendlyName: String): Either<Failure, Unit>
    suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit>
    suspend fun getLastFriendlyNameChanged(deviceId: String): Long
    suspend fun deleteDevice(serialNumber: String): Either<Failure, Unit>
}

class DeviceRepositoryImpl(
    private val deviceDataSource: DeviceDataSource,
    private val networkAddressDataSource: NetworkAddressDataSource,
    private val storageAddressDataSource: StorageAddressDataSource,
    private val userActionDataSource: UserActionDataSource
) : DeviceRepository {
    /*
    * First try to check if we have the address for this hex saved in the database.
    * If not, use Geocoder API (found in NetworkAddressDataSource) for reverse geocoding and then
    * save that address in the database.
     */
    override suspend fun getUserDevices(): Either<Failure, List<Device>> {
        return deviceDataSource.getUserDevices().map { devices ->
            devices.onEach {
                it.address = getDeviceAddress(it)
            }
        }
    }

    /*
    * First try to check if we have the address for this hex saved in the database.
    * If not, use Geocoder API (found in NetworkAddressDataSource) for reverse geocoding and then
    * save that address in the database.
     */
    override suspend fun getUserDevice(deviceId: String): Either<Failure, Device> {
        return deviceDataSource.getUserDevice(deviceId).map { device ->
            device.apply {
                this.address = getDeviceAddress(this)
            }
        }
    }

    override suspend fun claimDevice(
        serialNumber: String,
        location: Location,
        secret: String?
    ): Either<Failure, Device> {
        return deviceDataSource.claimDevice(serialNumber, location, secret)
    }

    override suspend fun getDeviceAddress(device: Device): String? {
        var hexAddress: String? = null

        device.attributes?.hex7?.let { hex ->
            storageAddressDataSource.getLocationAddress(hex.index, hex.center)
                .tap { address ->
                    Timber.d("Got location address from database [$address].")
                    hexAddress = address
                }
                .mapLeft {
                    networkAddressDataSource.getLocationAddress(hex.index, hex.center)
                        .tap { address ->
                            Timber.d("Got location address from network [$it].")
                            hexAddress = address
                            address?.let {
                                storageAddressDataSource.setLocationAddress(hex.index, it)
                            }
                        }
                }
        }

        return hexAddress
    }

    override suspend fun setFriendlyName(
        deviceId: String,
        friendlyName: String
    ): Either<Failure, Unit> {
        return deviceDataSource.setFriendlyName(deviceId, friendlyName)
            .tap {
                userActionDataSource.setLastFriendlyNameChanged(deviceId, Date().time)
            }
    }

    override suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit> {
        return deviceDataSource.clearFriendlyName(deviceId)
            .tap {
                userActionDataSource.setLastFriendlyNameChanged(deviceId, Date().time)
            }
    }

    override suspend fun getLastFriendlyNameChanged(deviceId: String): Long {
        return userActionDataSource.getLastFriendlyNameChanged(deviceId)
    }

    override suspend fun deleteDevice(serialNumber: String): Either<Failure, Unit> {
        return deviceDataSource.deleteDevice(serialNumber)
    }
}
