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
    // Devices on H3 and H7 hexes, the key is the hex index, and the value the contained devices
    private val devicesH3Hexes: MutableMap<String, MutableList<Device>> = mutableMapOf()
    private val devicesH7Hexes: MutableMap<String, MutableList<Device>> = mutableMapOf()

    suspend fun getUserDevices(): Either<Failure, List<Device>> {
        return deviceDataSource.getUserDevices()
    }

    suspend fun getUserDevice(deviceId: String): Either<Failure, Device> {
        return deviceDataSource.getUserDevice(deviceId)
    }

    suspend fun getPublicDevices(): Either<Failure, List<Device>> {
        return deviceDataSource.getPublicDevices().map {
            it.forEach { device ->
                device.attributes?.hex3?.let { hex ->
                    val deviceListForHex = devicesH3Hexes[hex.index]
                    devicesH3Hexes[hex.index] = if (deviceListForHex != null) {
                        deviceListForHex.add(device)
                        deviceListForHex
                    } else {
                        mutableListOf(device)
                    }
                }

                device.attributes?.hex7?.let { hex ->
                    val deviceListForHex = devicesH7Hexes[hex.index]
                    devicesH7Hexes[hex.index] = if (deviceListForHex != null) {
                        deviceListForHex.add(device)
                        deviceListForHex
                    } else {
                        mutableListOf(device)
                    }
                }
            }
            it
        }
    }

    suspend fun claimDevice(serialNumber: String, location: Location): Either<Failure, Device> {
        return deviceDataSource.claimDevice(serialNumber, location)
    }

    fun getDevicesOfH7(hexIndex: String?): MutableList<Device>? {
        return if (hexIndex == null) {
            mutableListOf()
        } else {
            return devicesH7Hexes[hexIndex]
        }
    }
}
