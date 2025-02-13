package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.WidgetRepository
import com.weatherxm.ui.common.UIDevice

class WidgetSelectStationUseCaseImpl(
    private val deviceRepository: DeviceRepository,
    private val widgetRepository: WidgetRepository
) : WidgetSelectStationUseCase {

    override suspend fun getUserDevices(): Either<Failure, List<UIDevice>> {
        return deviceRepository.getUserDevices().map {
            it.map { device ->
                device.toUIDevice()
            }
        }
    }

    override fun saveWidgetData(widgetId: Int, deviceId: String) {
        widgetRepository.setWidgetDevice(widgetId, deviceId)
        widgetRepository.setWidgetId(widgetId)
    }
}
