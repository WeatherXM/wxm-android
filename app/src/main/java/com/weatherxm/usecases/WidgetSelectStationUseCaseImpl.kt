package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.WidgetRepository

class WidgetSelectStationUseCaseImpl(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
    private val widgetRepository: WidgetRepository
) : WidgetSelectStationUseCase {

    override suspend fun isLoggedIn(): Either<Failure, Boolean> {
        return authRepository.isLoggedIn()
    }

    override suspend fun getUserDevices(): Either<Failure, List<Device>> {
        return deviceRepository.getUserDevices()
    }

    override fun saveWidgetData(widgetId: Int, deviceId: String) {
        widgetRepository.setWidgetDevice(widgetId, deviceId)
        widgetRepository.setWidgetId(widgetId)
    }
}
