package com.weatherxm.data.datasource

import android.content.SharedPreferences
import androidx.annotation.StringDef

interface UserActionDataSource {
    suspend fun getLastFriendlyNameChanged(deviceId: String): Long
    suspend fun setLastFriendlyNameChanged(deviceId: String, timestamp: Long)
    suspend fun clear()
}

class UserActionDataSourceImpl(
    private val preferences: SharedPreferences
) : UserActionDataSource {

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
        return preferences.getLong(key, 0L)
    }

    override suspend fun setLastFriendlyNameChanged(deviceId: String, timestamp: Long) {
        val key = getUserDeviceActionKey(ACTION_EDIT_FRIENDLY_NAME, deviceId)
        preferences.edit().putLong(key, timestamp).apply()
    }

    override suspend fun clear() {
        preferences.edit().clear().apply()
    }
}
