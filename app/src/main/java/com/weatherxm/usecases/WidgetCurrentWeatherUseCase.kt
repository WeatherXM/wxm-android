package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.Failure

interface WidgetCurrentWeatherUseCase {
    fun getDeviceOfWidget(widgetId: Int): Either<Failure, String>
    suspend fun getUserDevice(deviceId: String): Either<Failure, Device>
    suspend fun isLoggedIn(): Either<Failure, Boolean>
    fun removeWidgetId(widgetId: Int)
    suspend fun getUserDevices(deviceIds: List<String>): Either<Failure, List<Device>>
}
