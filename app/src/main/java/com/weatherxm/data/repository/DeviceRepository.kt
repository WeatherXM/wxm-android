package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.data.datasource.DeviceDataSource
import com.weatherxm.data.datasource.NetworkAddressDataSource
import com.weatherxm.data.datasource.StorageAddressDataSource
import timber.log.Timber

interface DeviceRepository {
    suspend fun getUserDevices(): Either<Failure, List<Device>>
    suspend fun getUserDevice(deviceId: String): Either<Failure, Device>
    suspend fun getPublicDevices(forceRefresh: Boolean): Either<Failure, List<Device>>
    suspend fun claimDevice(serialNumber: String, location: Location): Either<Failure, Device>
    suspend fun getDevicesOfH7(hexIndex: String?): MutableList<Device>
    suspend fun getDeviceAddress(device: Device): String?
    suspend fun setFriendlyName(deviceId: String, friendlyName: String): Either<Failure, Unit>
    suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit>
}

class DeviceRepositoryImpl(
    private val deviceDataSource: DeviceDataSource,
    private val networkAddressDataSource: NetworkAddressDataSource,
    private val storageAddressDataSource: StorageAddressDataSource
) : DeviceRepository {
    // Devices on H7 hexes, the key is the hex index, and the value the contained devices
    private val devicesH7Hexes: MutableMap<String, MutableList<Device>> = mutableMapOf()

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

    override suspend fun getPublicDevices(forceRefresh: Boolean): Either<Failure, List<Device>> {
        return deviceDataSource.getPublicDevices(forceRefresh).tap {
            devicesH7Hexes.clear()
            it.onEach { device ->
                device.attributes?.hex7?.let { hex ->
                    val deviceListForHex = devicesH7Hexes[hex.index]
                    devicesH7Hexes[hex.index] = deviceListForHex?.apply {
                        add(device)
                    } ?: mutableListOf(device)
                }
            }
        }
    }

    override suspend fun claimDevice(
        serialNumber: String,
        location: Location
    ): Either<Failure, Device> {
        return deviceDataSource.claimDevice(serialNumber, location)
    }

    override suspend fun getDevicesOfH7(hexIndex: String?): MutableList<Device> {
        return if (hexIndex == null) {
            mutableListOf()
        } else {
            val devices = devicesH7Hexes.getOrDefault(hexIndex, mutableListOf())
            devices.onEach {
                it.address = getDeviceAddress(it)
            }
        }
    }

    override suspend fun getDeviceAddress(device: Device): String? {
        var hexAddress: String? = null

        device.attributes?.hex7?.let { hex ->
            storageAddressDataSource.getLocationAddress(hex)
                .tap { address ->
                    Timber.d("Got location address from database [$address].")
                    hexAddress = address
                }
                .mapLeft {
                    networkAddressDataSource.getLocationAddress(hex).tap { address ->
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
    }

    override suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit> {
        return deviceDataSource.clearFriendlyName(deviceId)
    }
}
