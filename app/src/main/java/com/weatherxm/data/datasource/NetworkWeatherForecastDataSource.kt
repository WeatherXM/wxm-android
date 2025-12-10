package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.mapResponse
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
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
        exclude: String?,
        token: String?
    ): Either<Failure, List<WeatherData>> {
        return apiService.getForecast(
            deviceId,
            fromDate.toString(),
            toDate.toString(),
            exclude,
            token
        ).mapResponse()
    }

    override suspend fun getLocationForecast(
        location: Location
    ): Either<Failure, List<WeatherData>> {
        return apiService.getLocationForecast(location.lat, location.lon).mapResponse()
    }

    override suspend fun setDeviceForecast(deviceId: String, forecast: List<WeatherData>) {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun clearDeviceForecast() {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun setLocationForecast(location: Location, forecast: List<WeatherData>) {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override fun clearLocationForecast() {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }
}
