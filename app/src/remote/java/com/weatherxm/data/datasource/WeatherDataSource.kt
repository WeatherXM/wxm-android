package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.WeatherData
import com.weatherxm.data.map
import com.weatherxm.data.network.ApiService

interface WeatherDataSource {
    suspend fun getForecast(
        deviceId: String,
        fromDate: String,
        toDate: String,
        exclude: String?
    ): Either<Failure, List<WeatherData>>

    suspend fun getWeatherHistory(
        deviceId: String,
        fromDate: String,
        toDate: String,
        exclude: String?
    ): Either<Failure, List<WeatherData>>
}

class WeatherDataSourceImpl(
    private val apiService: ApiService
) : WeatherDataSource {

    override suspend fun getForecast(
        deviceId: String,
        fromDate: String,
        toDate: String,
        exclude: String?
    ): Either<Failure, List<WeatherData>> {
        return apiService.getForecast(deviceId, fromDate, toDate, exclude).map()
    }

    override suspend fun getWeatherHistory(
        deviceId: String,
        fromDate: String,
        toDate: String,
        exclude: String?
    ): Either<Failure, List<WeatherData>> {
        return apiService.getWeatherHistory(deviceId, fromDate, toDate, exclude).map()
    }
}
