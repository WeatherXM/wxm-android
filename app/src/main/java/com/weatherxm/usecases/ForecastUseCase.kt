package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIForecast

interface ForecastUseCase {
    suspend fun getForecast(
        device: UIDevice,
        forceRefresh: Boolean = false
    ): Either<Failure, UIForecast>
}
