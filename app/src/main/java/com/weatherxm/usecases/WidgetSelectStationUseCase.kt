package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.ui.common.UIDevice

interface WidgetSelectStationUseCase {
    suspend fun getUserDevices(): Either<Failure, List<UIDevice>>
    fun saveWidgetData(widgetId: Int, deviceId: String)
}
