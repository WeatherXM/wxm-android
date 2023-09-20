package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.ui.common.UIDevice

interface WidgetCurrentWeatherUseCase {
    suspend fun getUserDevice(deviceId: String): Either<Failure, UIDevice>
    fun removeWidgetId(widgetId: Int)

    fun getWidgetDevice(widgetId: Int): String?
}
