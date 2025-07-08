package com.weatherxm.ui.devicenotifications

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.DeviceNotificationsUseCase

class DeviceNotificationsViewModel(
    val device: UIDevice,
    private val useCase: DeviceNotificationsUseCase
) : ViewModel() {
    val areNotificationsEnabled = mutableStateOf(false)

    fun setDeviceNotificationsEnabled(enabled: Boolean) {
        areNotificationsEnabled.value = enabled
        useCase.setDeviceNotificationsEnabled(device.id, enabled)
    }

    fun getDeviceNotificationsEnabled(hasNotificationPermission: Boolean): Boolean {
        return useCase.getDeviceNotificationsEnabled(device.id).apply {
            areNotificationsEnabled.value = this && hasNotificationPermission
        }
    }
}
