package com.weatherxm.data.datasource

import androidx.annotation.StringDef
import com.weatherxm.data.services.CacheService

interface UserActionDataSource {
    suspend fun getLastFriendlyNameChanged(deviceId: String): Long
    suspend fun setLastFriendlyNameChanged(deviceId: String, timestamp: Long)
}

class UserActionDataSourceImpl(private val cacheService: CacheService) : UserActionDataSource {

    companion object {
        const val ACTION_EDIT_FRIENDLY_NAME = "edit_friendly_name"
    }

    @StringDef(value = [ACTION_EDIT_FRIENDLY_NAME])
    @Retention(AnnotationRetention.SOURCE)
    private annotation class UserAction

    private fun getUserDeviceActionKey(@UserAction action: String, deviceId: String): String {
        return "${action}_${deviceId}"
    }

    override suspend fun getLastFriendlyNameChanged(deviceId: String): Long {
        val key = getUserDeviceActionKey(ACTION_EDIT_FRIENDLY_NAME, deviceId)
        return cacheService.getLastFriendlyNameChanged(key)
    }

    override suspend fun setLastFriendlyNameChanged(deviceId: String, timestamp: Long) {
        val key = getUserDeviceActionKey(ACTION_EDIT_FRIENDLY_NAME, deviceId)
        cacheService.setLastFriendlyNameChanged(key, timestamp)
    }
}
