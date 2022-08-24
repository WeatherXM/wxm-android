package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather

interface WeatherHistoryDataSource {
    suspend fun getWeatherHistory(
        deviceId: String,
        fromDate: String,
        toDate: String,
        exclude: String?
    ): Either<Failure, List<HourlyWeather>>

    suspend fun setWeatherHistory(deviceId: String, weatherData: List<HourlyWeather>)
}
