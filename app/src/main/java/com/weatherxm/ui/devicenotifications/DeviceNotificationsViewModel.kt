package com.weatherxm.ui.devicenotifications

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.lifecycle.ViewModel
import com.weatherxm.data.models.DeviceNotificationType
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.DeviceNotificationsUseCase

class DeviceNotificationsViewModel(
    val device: UIDevice,
    private val useCase: DeviceNotificationsUseCase
) : ViewModel() {
    val notificationsEnabled = mutableStateOf(false)
    val notificationTypesEnabled = mutableStateSetOf<DeviceNotificationType>()

    fun setDeviceNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled.value = enabled
        useCase.setDeviceNotificationsEnabled(device.id, enabled)
    }

    fun getDeviceNotificationsEnabled(hasNotificationPermission: Boolean): Boolean {
        return useCase.getDeviceNotificationsEnabled(device.id).apply {
            notificationsEnabled.value = this && hasNotificationPermission
        }
    }

    fun getDeviceNotificationTypes() {
        notificationTypesEnabled.addAll(useCase.getDeviceNotificationTypesEnabled(device.id))
    }

    fun setDeviceNotificationTypeEnabled(type: DeviceNotificationType, enabled: Boolean) {
        if (enabled) {
            notificationTypesEnabled.add(type)
        } else {
            notificationTypesEnabled.remove(type)
        }
        useCase.setDeviceNotificationTypesEnabled(device.id, notificationTypesEnabled.toList())
    }
}
