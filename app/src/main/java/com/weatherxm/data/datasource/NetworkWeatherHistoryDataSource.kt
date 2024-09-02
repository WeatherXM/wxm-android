package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.mapResponse
import com.weatherxm.data.network.ApiService
import java.time.LocalDate

class NetworkWeatherHistoryDataSource(
    private val apiService: ApiService
) : WeatherHistoryDataSource {
    override suspend fun getWeatherHistory(
        deviceId: String,
        fromDate: LocalDate,
        toDate: LocalDate
    ): Either<Failure, List<HourlyWeather>> {
        return apiService.getWeatherHistory(deviceId, fromDate.toString(), toDate.toString())
            .mapResponse()
            .map { response ->
                response
                    .mapNotNull { it.hourly }
                    .flatten()
            }
    }

    override suspend fun setWeatherHistory(deviceId: String, data: List<HourlyWeather>) {
        throw NotImplementedError()
    }
}
