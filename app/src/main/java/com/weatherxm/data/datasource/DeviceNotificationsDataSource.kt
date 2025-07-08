package com.weatherxm.data.datasource

import com.weatherxm.data.services.CacheService
import com.weatherxm.data.services.CacheService.Companion.getDeviceNotificationsFormattedKey

interface DeviceNotificationsDataSource {
    fun setDeviceNotificationsEnabled(deviceId: String, enabled: Boolean)
    fun getDeviceNotificationsEnabled(deviceId: String): Boolean
}

class DeviceNotificationsDataSourceImpl(
    private val cacheService: CacheService
) : DeviceNotificationsDataSource {

    override fun setDeviceNotificationsEnabled(deviceId: String, enabled: Boolean) {
        val key = getDeviceNotificationsFormattedKey(deviceId)
        cacheService.setDeviceNotificationsEnabled(key, enabled)
    }

    override fun getDeviceNotificationsEnabled(deviceId: String): Boolean {
        val key = getDeviceNotificationsFormattedKey(deviceId)
        return cacheService.getDeviceNotificationsEnabled(key)
    }
}
