package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.WeatherData
import com.weatherxm.data.datasource.CacheWeatherForecastDataSource
import com.weatherxm.data.datasource.NetworkWeatherForecastDataSource
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

        return cacheSource.getForecast(deviceId, fromDate, to)
            .onRight {
                Timber.d("Got forecast from cache [$fromDate to $to].")
            }
            .mapLeft {
                return networkSource.getForecast(deviceId, fromDate, to).onRight {
                    Timber.d("Got forecast from network [$fromDate to $to].")
                    cacheSource.setForecast(deviceId, it)
                }
            }
    }

    override suspend fun clearCache() {
        cacheSource.clear()
    }
}
