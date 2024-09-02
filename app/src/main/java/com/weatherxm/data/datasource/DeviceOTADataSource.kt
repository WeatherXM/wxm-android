package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.mapResponse
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.services.CacheService

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
        return apiService.getFirmware(deviceId).mapResponse().map {
            it.bytes()
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
        cacheService.setDeviceLastOtaTimestamp(key)
    }

    override fun getDeviceLastOtaTimestamp(deviceId: String): Long {
        val key = getDeviceFormattedKey(LAST_OTA_TIMESTAMP, deviceId)
        return cacheService.getDeviceLastOtaTimestamp(key)
    }
}
