package com.weatherxm.data.datasource

import com.weatherxm.data.services.CacheService
import com.weatherxm.data.services.CacheService.Companion.getDeviceNotificationTypesFormattedKey
import com.weatherxm.data.services.CacheService.Companion.getDeviceNotificationsFormattedKey

interface DeviceNotificationsDataSource {
    fun setDeviceNotificationsEnabled(deviceId: String, enabled: Boolean)
    fun getDeviceNotificationsEnabled(deviceId: String): Boolean
    fun setDeviceNotificationTypesEnabled(deviceId: String, types: Set<String>)
    fun getDeviceNotificationTypesEnabled(deviceId: String): Set<String>
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

    override fun setDeviceNotificationTypesEnabled(deviceId: String, types: Set<String>) {
        val key = getDeviceNotificationTypesFormattedKey(deviceId)
        cacheService.setDeviceNotificationTypesEnabled(key, types)
    }

    override fun getDeviceNotificationTypesEnabled(deviceId: String): Set<String> {
        val key = getDeviceNotificationTypesFormattedKey(deviceId)
        return cacheService.getDeviceNotificationTypesEnabled(key)
    }
}
