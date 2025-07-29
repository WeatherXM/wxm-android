package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIForecast

interface ForecastUseCase {
    suspend fun getDeviceForecast(
        device: UIDevice,
        forceRefresh: Boolean = false
    ): Either<Failure, UIForecast>

    suspend fun getLocationForecast(location: Location): Either<Failure, UIForecast>
}
