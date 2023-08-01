package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.WidgetRepository
import com.weatherxm.ui.common.DeviceOwnershipStatus
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
            it.toUIDevice().apply {
                // TODO: Remove this when we have the API info in the response
                this.ownershipStatus = DeviceOwnershipStatus.OWNED
            }
        }
    }

    override suspend fun getUserDevices(
        deviceIds: List<String>,
        numOfRetries: Int
    ): Either<Failure, List<UIDevice>> {
        return deviceRepository.getUserDevices(deviceIds).map {
            it.map { device ->
                device.toUIDevice().apply {
                    // TODO: Remove this when we have the API info in the response
                    this.ownershipStatus = DeviceOwnershipStatus.OWNED
                }
            }
        }
    }
}
