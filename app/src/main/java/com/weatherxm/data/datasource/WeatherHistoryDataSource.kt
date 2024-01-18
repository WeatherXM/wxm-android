package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather
import java.time.LocalDate

interface WeatherHistoryDataSource {
    suspend fun getWeatherHistory(
        deviceId: String,
        fromDate: LocalDate,
        toDate: LocalDate
    ): Either<Failure, List<HourlyWeather>>

    suspend fun setWeatherHistory(
        deviceId: String,
        data: List<HourlyWeather>
    )
}
