package com.weatherxm.data.datasource

import arrow.core.Either
import arrow.core.flatMap
import com.weatherxm.data.mapResponse
import com.weatherxm.data.models.Failure
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.services.CacheService
import timber.log.Timber
import java.io.IOException

interface DeviceOTADataSource {
    suspend fun getFirmware(deviceId: String): Either<Failure, ByteArray>
    fun setDeviceLastOtaVersion(deviceId: String, otaVersion: String)
    fun getDeviceLastOtaVersion(deviceId: String): Either<Failure, String>
    fun setDeviceLastOtaTimestamp(deviceId: String)
    fun getDeviceLastOtaTimestamp(deviceId: String): Long
}

class DeviceOTADataSourceImpl(
    private val apiService: ApiService,
    private val cacheService: CacheService
) : DeviceOTADataSource {
    companion object {
        const val LAST_OTA_VERSION = "last_ota_version"
        const val LAST_OTA_TIMESTAMP = "last_ota_timestamp"
    }

    override suspend fun getFirmware(deviceId: String): Either<Failure, ByteArray> {
        return apiService.getFirmware(deviceId).mapResponse().flatMap {
            try {
                Either.Right(it.bytes())
            } catch (e: IOException) {
                Timber.e(e)
                Either.Left(Failure.FirmwareBytesParsingError)
            }
        }
    }

    private fun getDeviceFormattedKey(keyPrefix: String, deviceId: String): String {
        return "${keyPrefix}_${deviceId}"
    }

    override fun getDeviceLastOtaVersion(deviceId: String): Either<Failure, String> {
        val key = getDeviceFormattedKey(LAST_OTA_VERSION, deviceId)
        return cacheService.getDeviceLastOtaVersion(key)
    }

    override fun setDeviceLastOtaVersion(deviceId: String, otaVersion: String) {
        val key = getDeviceFormattedKey(LAST_OTA_VERSION, deviceId)
        cacheService.setDeviceLastOtaVersion(key, otaVersion)
    }

    override fun setDeviceLastOtaTimestamp(deviceId: String) {
        val key = getDeviceFormattedKey(LAST_OTA_TIMESTAMP, deviceId)
        cacheService.setDeviceLastOtaTimestamp(key, System.currentTimeMillis())
    }

    override fun getDeviceLastOtaTimestamp(deviceId: String): Long {
        val key = getDeviceFormattedKey(LAST_OTA_TIMESTAMP, deviceId)
        return cacheService.getDeviceLastOtaTimestamp(key)
    }
}
