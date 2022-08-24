package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.WeatherData
import com.weatherxm.data.datasource.CacheWeatherForecastDataSource
import com.weatherxm.data.datasource.NetworkWeatherForecastDataSource
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import org.koin.core.component.KoinComponent
import timber.log.Timber
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

interface WeatherForecastRepository {
    suspend fun getDeviceForecast(
        deviceId: String,
        fromDate: ZonedDateTime,
        toDate: ZonedDateTime,
        forceRefresh: Boolean
    ): Either<Failure, List<WeatherData>>

    suspend fun clearCache()
}

class WeatherForecastRepositoryImpl(
    private val networkSource: NetworkWeatherForecastDataSource,
    private val cacheSource: CacheWeatherForecastDataSource,
) : WeatherForecastRepository, KoinComponent {

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

        val from = getFormattedDate(fromDate)
        val to = if (ChronoUnit.DAYS.between(fromDate, toDate) < PREFETCH_DAYS) {
            getFormattedDate(fromDate.plusDays(PREFETCH_DAYS))
        } else {
            getFormattedDate(toDate)
        }

        return cacheSource.getForecast(deviceId, from, to)
            .tap {
                Timber.d("Got forecast from cache [$from to $to].")
            }
            .mapLeft {
                return networkSource.getForecast(deviceId, from, to).tap {
                    Timber.d("Got forecast from network [$from to $to].")
                    cacheSource.setForecast(deviceId, it)
                }
            }
    }

    override suspend fun clearCache() {
        cacheSource.clear()
    }
}
