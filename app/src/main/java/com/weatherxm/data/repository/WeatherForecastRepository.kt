package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.datasource.CacheWeatherForecastDataSource
import com.weatherxm.data.datasource.NetworkWeatherForecastDataSource
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
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

    fun clearLocationForecastFromCache()
    suspend fun getLocationForecast(location: Location): Either<Failure, List<WeatherData>>
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
            clearDeviceForecastFromCache()
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

    override fun clearLocationForecastFromCache() {
        cacheSource.clearLocationForecast()
    }

    private suspend fun clearDeviceForecastFromCache() {
        cacheSource.clearDeviceForecast()
    }

    override suspend fun getLocationForecast(
        location: Location
    ): Either<Failure, List<WeatherData>> {
        return cacheSource.getLocationForecast(location).onRight {
            Timber.d("Got forecast from cache for location [$location].")
        }.mapLeft {
            return networkSource.getLocationForecast(location).onRight {
                Timber.d("Got forecast from network for location [$location].")
                cacheSource.setLocationForecast(location, it)
            }
        }
    }
}
