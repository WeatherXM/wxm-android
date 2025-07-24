package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.mapResponse
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.WeatherData
import com.weatherxm.data.network.ApiService
import java.time.LocalDate

class NetworkWeatherForecastDataSource(
    private val apiService: ApiService
) : WeatherForecastDataSource {

    override suspend fun getDeviceForecast(
        deviceId: String,
        fromDate: LocalDate,
        toDate: LocalDate,
        exclude: String?
    ): Either<Failure, List<WeatherData>> {
        return apiService.getForecast(
            deviceId,
            fromDate.toString(),
            toDate.toString(),
            exclude
        ).mapResponse()
    }

    override suspend fun setDeviceForecast(deviceId: String, forecast: List<WeatherData>) {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun clear() {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun getLocationForecast(
        lat: Double,
        lon: Double
    ): Either<Failure, List<WeatherData>> {
        return apiService.getLocationForecast(lat, lon).mapResponse()
    }
}
