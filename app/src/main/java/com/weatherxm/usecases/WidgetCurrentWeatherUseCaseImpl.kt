package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.WidgetRepository

class WidgetCurrentWeatherUseCaseImpl(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
    private val widgetRepository: WidgetRepository
) : WidgetCurrentWeatherUseCase {

    override suspend fun isLoggedIn(): Either<Failure, Boolean> {
        return authRepository.isLoggedIn()
    }

    override fun removeWidgetId(widgetId: Int) {
        widgetRepository.removeWidgetId(widgetId)
    }

    override suspend fun getUserDevice(deviceId: String): Either<Failure, Device> {
        return deviceRepository.getUserDevice(deviceId)
    }

    override fun getDeviceOfWidget(widgetId: Int): Either<Failure, String> {
        return widgetRepository.getDeviceOfWidget(widgetId)
    }

    override suspend fun getUserDevices(deviceIds: List<String>): Either<Failure, List<Device>> {
        return deviceRepository.getUserDevices(deviceIds)
    }
}
