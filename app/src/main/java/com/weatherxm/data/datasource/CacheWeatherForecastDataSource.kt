package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.locationToText
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.WeatherData
import com.weatherxm.data.services.CacheService
import java.time.LocalDate

class CacheWeatherForecastDataSource(
    private val cacheService: CacheService
) : WeatherForecastDataSource {

    override suspend fun getDeviceForecast(
        deviceId: String,
        fromDate: LocalDate,
        toDate: LocalDate,
        exclude: String?
    ): Either<Failure, List<WeatherData>> {
        return cacheService.getDeviceForecast(deviceId)
    }

    override suspend fun setDeviceForecast(deviceId: String, forecast: List<WeatherData>) {
        cacheService.setDeviceForecast(deviceId, forecast)
    }

    override suspend fun clearDeviceForecast() {
        cacheService.clearDeviceForecast()
    }

    override suspend fun getLocationForecast(location: Location): Either<Failure, List<WeatherData>> {
        return cacheService.getLocationForecast(location.locationToText())
    }

    override suspend fun setLocationForecast(location: Location, forecast: List<WeatherData>) {
        cacheService.setLocationForecast(location.locationToText(), forecast)
    }

    override fun clearLocationForecast() {
        cacheService.clearLocationForecast()
    }
}
