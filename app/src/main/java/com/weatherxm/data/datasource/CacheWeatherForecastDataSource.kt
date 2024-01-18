package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.WeatherData
import com.weatherxm.data.services.CacheService
import java.time.ZonedDateTime

class CacheWeatherForecastDataSource(
    private val cacheService: CacheService
) : WeatherForecastDataSource {

    override suspend fun getForecast(
        deviceId: String,
        fromDate: ZonedDateTime,
        toDate: ZonedDateTime,
        exclude: String?
    ): Either<Failure, List<WeatherData>> {
        return cacheService.getForecast(deviceId)
    }

    override suspend fun setForecast(deviceId: String, forecast: List<WeatherData>) {
        cacheService.setForecast(deviceId, forecast)
    }

    override suspend fun clear() {
        cacheService.clearForecast()
    }
}
