package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.Failure

interface WidgetCurrentWeatherUseCase {
    suspend fun getUserDevice(deviceId: String): Either<Failure, Device>
    fun removeWidgetId(widgetId: Int)
    suspend fun getUserDevices(
        deviceIds: List<String>,
        numOfRetries: Int = 0
    ): Either<Failure, List<Device>>

    fun getWidgetDevice(widgetId: Int): String?
}
