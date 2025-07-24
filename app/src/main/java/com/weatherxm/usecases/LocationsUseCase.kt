package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.ui.common.LocationWeather

interface LocationsUseCase {
    suspend fun getLocationWeather(location: Location): Either<Failure, LocationWeather>
    fun getSavedLocations(): List<Location>
    fun clearLocationForecastFromCache()
    fun addSavedLocation(location: Location)
    fun removeSavedLocation(location: Location)
}
