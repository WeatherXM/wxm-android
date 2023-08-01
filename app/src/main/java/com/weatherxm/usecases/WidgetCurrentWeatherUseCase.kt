package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.ui.common.UIDevice

interface WidgetCurrentWeatherUseCase {
    suspend fun getUserDevice(deviceId: String): Either<Failure, UIDevice>
    fun removeWidgetId(widgetId: Int)
    suspend fun getUserDevices(
        deviceIds: List<String>,
        numOfRetries: Int = 0
    ): Either<Failure, List<UIDevice>>

    fun getWidgetDevice(widgetId: Int): String?
}
