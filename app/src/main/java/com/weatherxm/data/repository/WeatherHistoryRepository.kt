package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather
import java.time.LocalDate

interface WeatherHistoryRepository {
    suspend fun getHourlyWeatherHistory(
        deviceId: String,
        date: LocalDate,
        forceUpdate: Boolean = false
    ): Either<Failure, List<HourlyWeather>>
}
