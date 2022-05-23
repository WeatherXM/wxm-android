package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.WeatherData
import com.weatherxm.data.datasource.CacheWeatherDataSource
import com.weatherxm.data.datasource.NetworkWeatherDataSource
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import timber.log.Timber
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

interface WeatherRepository {
    suspend fun getDeviceForecast(
        deviceId: String,
        fromDate: ZonedDateTime,
        toDate: ZonedDateTime,
        forceRefresh: Boolean
    ): Either<Failure, List<WeatherData>>

    suspend fun clearCache()
    suspend fun getHourlyWeatherHistory(
        deviceId: String,
        fromDate: String,
        toDate: String
    ): Either<Failure, List<WeatherData>>
}

class WeatherRepositoryImpl(
    private val networkWeatherDataSource: NetworkWeatherDataSource,
    private val cacheWeatherDataSource: CacheWeatherDataSource
) : WeatherRepository {

    companion object {
        const val PREFETCH_DAYS = 7L
    }

    override suspend fun getDeviceForecast(
        deviceId: String,
        fromDate: ZonedDateTime,
        toDate: ZonedDateTime,
        forceRefresh: Boolean
    ): Either<Failure, List<WeatherData>> {
        if (forceRefresh) {
            clearCache()
        }

        val from = getFormattedDate(fromDate.toString())
        val to = if (ChronoUnit.DAYS.between(fromDate, toDate) < PREFETCH_DAYS) {
            getFormattedDate(fromDate.plusDays(PREFETCH_DAYS).toString())
        } else {
            getFormattedDate(toDate.toString())
        }

        return cacheWeatherDataSource.getForecast(deviceId, from, to)
            .tap {
                Timber.d("Got forecast from cache [$from to $to].")
            }
            .mapLeft {
                return networkWeatherDataSource.getForecast(deviceId, from, to).tap {
                    Timber.d("Got forecast from network [$from to $to].")
                    cacheWeatherDataSource.setForecast(deviceId, it)
                }
            }
    }

    override suspend fun clearCache() {
        cacheWeatherDataSource.clear()
    }

    override suspend fun getHourlyWeatherHistory(
        deviceId: String,
        fromDate: String,
        toDate: String
    ): Either<Failure, List<WeatherData>> {
        return networkWeatherDataSource.getWeatherHistory(deviceId, fromDate, toDate, "daily")
    }
}
