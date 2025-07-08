package com.weatherxm.usecases

import com.weatherxm.data.models.DeviceNotificationType
import com.weatherxm.data.repository.DeviceNotificationsRepository

interface DeviceNotificationsUseCase {
    fun setDeviceNotificationsEnabled(deviceId: String, enabled: Boolean)
    fun getDeviceNotificationsEnabled(deviceId: String): Boolean
    fun getDeviceNotificationTypesEnabled(deviceId: String): List<DeviceNotificationType>
    fun setDeviceNotificationTypesEnabled(
        deviceId: String,
        types: List<DeviceNotificationType>
    )
}

class DeviceNotificationsUseCaseImpl(
    private val repository: DeviceNotificationsRepository
) : DeviceNotificationsUseCase {

    override fun setDeviceNotificationsEnabled(deviceId: String, enabled: Boolean) {
        repository.setDeviceNotificationsEnabled(deviceId, enabled)
    }

    override fun getDeviceNotificationsEnabled(deviceId: String): Boolean {
        return repository.getDeviceNotificationsEnabled(deviceId)
    }

    override fun getDeviceNotificationTypesEnabled(deviceId: String): List<DeviceNotificationType> {
        return repository.getDeviceNotificationTypesEnabled(deviceId)
    }

    override fun setDeviceNotificationTypesEnabled(
        deviceId: String,
        types: List<DeviceNotificationType>
    ) {
        repository.setDeviceNotificationTypesEnabled(deviceId, types)
    }
}
