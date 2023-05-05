package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.Failure

interface WidgetSelectStationUseCase {
    suspend fun getUserDevices(): Either<Failure, List<Device>>
    suspend fun isLoggedIn(): Either<Failure, Boolean>
    fun saveWidgetData(widgetId: Int, deviceId: String)
}
