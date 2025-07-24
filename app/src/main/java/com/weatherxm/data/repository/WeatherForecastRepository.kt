package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.datasource.CacheWeatherForecastDataSource
import com.weatherxm.data.datasource.NetworkWeatherForecastDataSource
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.WeatherData
import timber.log.Timber
import java.time.LocalDate
import java.time.temporal.ChronoUnit

interface WeatherForecastRepository {
    suspend fun getDeviceForecast(
        deviceId: String,
        fromDate: LocalDate,
        toDate: LocalDate,
        forceRefresh: Boolean
    ): Either<Failure, List<WeatherData>>

    suspend fun clearCache()
    suspend fun getLocationForecast(
        lat: Double,
        lon: Double
    ): Either<Failure, List<WeatherData>>
}

class WeatherForecastRepositoryImpl(
    private val networkSource: NetworkWeatherForecastDataSource,
    private val cacheSource: CacheWeatherForecastDataSource,
) : WeatherForecastRepository {

    companion object {
        const val PREFETCH_DAYS = 7L
    }

    override suspend fun getDeviceForecast(
        deviceId: String,
        fromDate: LocalDate,
        toDate: LocalDate,
        forceRefresh: Boolean
    ): Either<Failure, List<WeatherData>> {
        if (forceRefresh) {
            clearCache()
        }

        val to = if (ChronoUnit.DAYS.between(fromDate, toDate) < PREFETCH_DAYS) {
            fromDate.plusDays(PREFETCH_DAYS)
        } else {
            toDate
        }

        return cacheSource.getDeviceForecast(deviceId, fromDate, to)
            .onRight {
                Timber.d("Got forecast from cache [$fromDate to $to].")
            }
            .mapLeft {
                return networkSource.getDeviceForecast(deviceId, fromDate, to).onRight {
                    Timber.d("Got forecast from network [$fromDate to $to].")
                    cacheSource.setDeviceForecast(deviceId, it)
                }
            }
    }

    override suspend fun clearCache() {
        cacheSource.clear()
    }

    override suspend fun getLocationForecast(
        lat: Double,
        lon: Double
    ): Either<Failure, List<WeatherData>> {
        // TODO: STOPSHIP: Use cache as an optimization like above 
        return networkSource.getLocationForecast(lat, lon)
    }
}
