package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.WidgetRepository

class WidgetCurrentWeatherUseCaseImpl(
    private val deviceRepository: DeviceRepository,
    private val widgetRepository: WidgetRepository
) : WidgetCurrentWeatherUseCase {
    override fun removeWidgetId(widgetId: Int) {
        widgetRepository.removeWidgetId(widgetId)
    }

    override fun getWidgetDevice(widgetId: Int): String? {
        return widgetRepository.getWidgetDevice(widgetId).getOrNull()
    }

    override suspend fun getUserDevice(deviceId: String): Either<Failure, Device> {
        return deviceRepository.getUserDevice(deviceId)
    }

    override suspend fun getUserDevices(
        deviceIds: List<String>,
        numOfRetries: Int
    ): Either<Failure, List<Device>> {
        return deviceRepository.getUserDevices(deviceIds)
    }
}
