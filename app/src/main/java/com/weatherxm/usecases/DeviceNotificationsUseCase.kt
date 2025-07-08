package com.weatherxm.usecases

import com.weatherxm.data.repository.DeviceNotificationsRepository

interface DeviceNotificationsUseCase {
    fun setDeviceNotificationsEnabled(deviceId: String, enabled: Boolean)
    fun getDeviceNotificationsEnabled(deviceId: String): Boolean
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
}
