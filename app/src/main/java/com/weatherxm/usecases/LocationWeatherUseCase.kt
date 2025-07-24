package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.ui.common.LocationWeather

interface LocationWeatherUseCase {
    suspend fun getLocationWeather(lat: Double, lon: Double): Either<Failure, LocationWeather>
}
