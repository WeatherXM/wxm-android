package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.datasource.DeviceOTADataSource
import java.util.concurrent.TimeUnit

interface DeviceOTARepository {
    suspend fun getFirmware(deviceId: String): Either<Failure, ByteArray>
    fun onUpdateSuccess(deviceId: String, otaVersion: String)
    fun shouldShowOTAPrompt(deviceId: String, assignedOtaVersion: String?): Boolean
}

class DeviceOTARepositoryImpl(
    private val deviceOTADataSource: DeviceOTADataSource
) : DeviceOTARepository {
    companion object {
        val OTA_UPDATE_HIDE_EXPIRATION = TimeUnit.HOURS.toMillis(1L)
    }

    override suspend fun getFirmware(deviceId: String): Either<Failure, ByteArray> {
        return deviceOTADataSource.getFirmware(deviceId)
    }

    override fun onUpdateSuccess(deviceId: String, otaVersion: String) {
        deviceOTADataSource.setDeviceLastOtaVersion(deviceId, otaVersion)
        deviceOTADataSource.setDeviceLastOtaTimestamp(deviceId)
    }

    override fun shouldShowOTAPrompt(deviceId: String, assignedOtaVersion: String?): Boolean {
        return if (assignedOtaVersion.isNullOrEmpty()) {
            false
        } else {
            deviceOTADataSource.getDeviceLastOtaVersion(deviceId).fold({ true }, {
                val lastOtaTimestamp = deviceOTADataSource.getDeviceLastOtaTimestamp(deviceId)
                val now = System.currentTimeMillis()
                it != assignedOtaVersion || (now - lastOtaTimestamp) > OTA_UPDATE_HIDE_EXPIRATION
            })
        }
    }
}
