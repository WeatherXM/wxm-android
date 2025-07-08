package com.weatherxm.data.repository

import com.weatherxm.data.datasource.DeviceNotificationsDataSource

interface DeviceNotificationsRepository {
    fun setDeviceNotificationsEnabled(deviceId: String, enabled: Boolean)
    fun getDeviceNotificationsEnabled(deviceId: String): Boolean
}

class DeviceNotificationsRepositoryImpl(
    private val datasource: DeviceNotificationsDataSource
) : DeviceNotificationsRepository {

    override fun setDeviceNotificationsEnabled(deviceId: String, enabled: Boolean) {
        datasource.setDeviceNotificationsEnabled(deviceId, enabled)
    }

    override fun getDeviceNotificationsEnabled(deviceId: String): Boolean {
        return datasource.getDeviceNotificationsEnabled(deviceId)
    }
}
