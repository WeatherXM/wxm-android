package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.WeatherData
import com.weatherxm.data.datasource.WeatherDataSource
import org.koin.core.component.KoinComponent

class WeatherRepository(private val weatherDataSource: WeatherDataSource) : KoinComponent {

    suspend fun getForecast(
        deviceId: String,
        fromDate: String,
        toDate: String
    ): Either<Failure, List<WeatherData>> {
        return weatherDataSource.getForecast(deviceId, fromDate, toDate, null)
    }

    suspend fun getHourlyForecast(
        deviceId: String,
        fromDate: String,
        toDate: String
    ): Either<Failure, List<WeatherData>> {
        return weatherDataSource.getForecast(deviceId, fromDate, toDate, "daily")
    }

    suspend fun getDailyForecast(
        deviceId: String,
        fromDate: String,
        toDate: String
    ): Either<Failure, List<WeatherData>> {
        return weatherDataSource.getForecast(deviceId, fromDate, toDate, "hourly")
    }
}
