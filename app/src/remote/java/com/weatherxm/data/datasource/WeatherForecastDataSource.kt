package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.WeatherData

interface WeatherForecastDataSource {
    suspend fun getForecast(
        deviceId: String,
        fromDate: String,
        toDate: String,
        exclude: String? = null
    ): Either<Failure, List<WeatherData>>

    suspend fun setForecast(deviceId: String, forecast: List<WeatherData>)

    suspend fun clear()
}
