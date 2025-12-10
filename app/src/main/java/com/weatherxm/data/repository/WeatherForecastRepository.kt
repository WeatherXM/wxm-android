package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.datasource.CacheWeatherForecastDataSource
import com.weatherxm.data.datasource.NetworkWeatherForecastDataSource
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.WeatherData
import com.weatherxm.service.BillingService
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
    private val billingService: BillingService,
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
        val to = if (ChronoUnit.DAYS.between(fromDate, toDate) < PREFETCH_DAYS) {
            fromDate.plusDays(PREFETCH_DAYS)
        } else {
            toDate
        }

        // TODO: STOPSHIP: Revert the below with billingService.hasActiveSub()
        return if (true) {
            getDevicePremiumForecast(deviceId, fromDate, to)
        } else {
            getDeviceDefaultForecast(deviceId, fromDate, to, forceRefresh)
        }
    }

    private suspend fun getDeviceDefaultForecast(
        deviceId: String,
        fromDate: LocalDate,
        toDate: LocalDate,
        forceRefresh: Boolean
    ): Either<Failure, List<WeatherData>> {
        if (forceRefresh) {
            clearDeviceForecastFromCache()
        }

        return cacheSource.getDeviceForecast(deviceId, fromDate, toDate)
            .onRight {
                Timber.d("Got forecast from cache [$fromDate to $toDate].")
            }
            .mapLeft {
                return networkSource.getDeviceForecast(deviceId, fromDate, toDate).onRight {
                    Timber.d("Got forecast from network [$fromDate to $toDate].")
                    cacheSource.setDeviceForecast(deviceId, it)
                }
            }
    }

    private suspend fun getDevicePremiumForecast(
        deviceId: String,
        fromDate: LocalDate,
        toDate: LocalDate
    ): Either<Failure, List<WeatherData>> {
        val token = billingService.getActiveSubFlow().value?.purchaseToken
        return networkSource.getDeviceForecast(deviceId, fromDate, toDate, token = token).onRight {
            Timber.d("Got premium forecast from network [$fromDate to $toDate].")
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
