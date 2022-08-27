package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather

interface WeatherHistoryRepository {
    suspend fun getHourlyWeatherHistory(
        deviceId: String,
        fromDate: String,
        toDate: String
    ): Either<Failure, List<HourlyWeather>>
}
