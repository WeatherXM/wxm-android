package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.data.datasource.DeviceDataSource
import org.koin.core.component.KoinComponent

class DeviceRepository(
    private val deviceDataSource: DeviceDataSource
) : KoinComponent {
    // Devices on H7 hexes, the key is the hex index, and the value the contained devices
    private val devicesH7Hexes: MutableMap<String, MutableList<Device>> = mutableMapOf()

    suspend fun getUserDevices(): Either<Failure, List<Device>> {
        return deviceDataSource.getUserDevices()
    }

    suspend fun getUserDevice(deviceId: String): Either<Failure, Device> {
        return deviceDataSource.getUserDevice(deviceId)
    }

    suspend fun getPublicDevices(forceRefresh: Boolean): Either<Failure, List<Device>> {
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

    suspend fun claimDevice(serialNumber: String, location: Location): Either<Failure, Device> {
        return deviceDataSource.claimDevice(serialNumber, location)
    }

    fun getDevicesOfH7(hexIndex: String?): MutableList<Device> {
        return if (hexIndex == null) {
            mutableListOf()
        } else {
            return devicesH7Hexes.getOrDefault(hexIndex, mutableListOf())
        }
    }
}
