package com.weatherxm.data.repository

import com.weatherxm.data.datasource.DeviceNotificationsDataSource
import com.weatherxm.data.models.DeviceNotificationType

interface DeviceNotificationsRepository {
    fun setDeviceNotificationsEnabled(deviceId: String, enabled: Boolean)
    fun getDeviceNotificationsEnabled(deviceId: String): Boolean
    fun getDeviceNotificationTypesEnabled(deviceId: String): Set<DeviceNotificationType>
    fun setDeviceNotificationTypesEnabled(
        deviceId: String,
        types: Set<DeviceNotificationType>
    )

    fun showDeviceNotificationsPrompt(): Boolean
    fun checkDeviceNotificationsPrompt()
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

    override fun setDeviceNotificationTypesEnabled(
        deviceId: String,
        types: Set<DeviceNotificationType>
    ) {
        datasource.setDeviceNotificationTypesEnabled(
            deviceId = deviceId,
            types = types.map { it.name }.toSet()
        )
    }

    override fun getDeviceNotificationTypesEnabled(deviceId: String): Set<DeviceNotificationType> {
        return datasource.getDeviceNotificationTypesEnabled(deviceId).map {
            DeviceNotificationType.valueOf(it)
        }.toSet()
    }

    override fun showDeviceNotificationsPrompt() = datasource.showDeviceNotificationsPrompt()
    override fun checkDeviceNotificationsPrompt() = datasource.checkDeviceNotificationsPrompt()
}
