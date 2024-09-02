package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.WeatherData
import com.weatherxm.data.mapResponse
import com.weatherxm.data.network.ApiService
import java.time.ZonedDateTime

class NetworkWeatherForecastDataSource(
    private val apiService: ApiService
) : WeatherForecastDataSource {

    override suspend fun getForecast(
        deviceId: String,
        fromDate: ZonedDateTime,
        toDate: ZonedDateTime,
        exclude: String?
    ): Either<Failure, List<WeatherData>> {
        return apiService.getForecast(
            deviceId,
            fromDate.toLocalDate().toString(),
            toDate.toLocalDate().toString(),
            exclude
        ).mapResponse()
    }

    override suspend fun setForecast(deviceId: String, forecast: List<WeatherData>) {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun clear() {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }
}
