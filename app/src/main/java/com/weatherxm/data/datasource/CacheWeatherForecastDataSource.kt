package com.weatherxm.data.datasource

import androidx.collection.ArrayMap
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.rightIfNotNull
import com.weatherxm.data.DataError
import com.weatherxm.data.Failure
import com.weatherxm.data.WeatherData
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * In-memory user cache. Could be expanded to use SharedPreferences or a different cache.
 */
class CacheWeatherForecastDataSource : WeatherForecastDataSource {

    companion object {
        // Default cache expiration time 15 minutes
        val DEFAULT_CACHE_EXPIRATION = TimeUnit.MINUTES.toMillis(15L)
    }

    private var forecasts: ArrayMap<String, TimedForecastData> = ArrayMap()

    override suspend fun getForecast(
        deviceId: String,
        fromDate: ZonedDateTime,
        toDate: ZonedDateTime,
        exclude: String?
    ): Either<Failure, List<WeatherData>> {
        return forecasts[deviceId].rightIfNotNull {
            DataError.CacheMissError
        }.flatMap {
            if (it.isExpired()) {
                Either.Left(DataError.CacheExpiredError)
            } else {
                Either.Right(it.value)
            }
        }
    }

    override suspend fun setForecast(deviceId: String, forecast: List<WeatherData>) {
        this.forecasts[deviceId] = TimedForecastData(forecast)
    }

    override suspend fun clear() {
        this.forecasts.clear()
    }

    data class TimedForecastData(
        val value: List<WeatherData>,
        private val cacheExpirationTime: Long = DEFAULT_CACHE_EXPIRATION
    ) {
        private val creationTime: Long = now()

        fun isExpired() = (now() - creationTime) > cacheExpirationTime

        private fun now() = System.currentTimeMillis()
    }
}
