package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.WidgetRepository
import com.weatherxm.ui.common.UIDevice

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

    override suspend fun getUserDevice(deviceId: String): Either<Failure, UIDevice> {
        return deviceRepository.getUserDevice(deviceId).map {
            it.toUIDevice()
        }
    }
}
