package com.weatherxm.data.datasource

import androidx.collection.ArrayMap
import arrow.core.Either
import com.weatherxm.data.DataError
import com.weatherxm.data.Failure
import com.weatherxm.data.WeatherData
import com.weatherxm.data.map
import com.weatherxm.data.network.ApiService
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class NetworkWeatherForecastDataSource(private val apiService: ApiService) :
    WeatherForecastDataSource {

    override suspend fun getForecast(
        deviceId: String,
        fromDate: String,
        toDate: String,
        exclude: String?
    ): Either<Failure, List<WeatherData>> {
        val response = apiService.getForecast(deviceId, fromDate, toDate, exclude).map()
        response.map { data ->
            val tz = ZoneId.of(data[0].tz)
            val todayDate = ZonedDateTime.now(tz)

            data[0].date = getFormattedDate(todayDate)
            data[1].date = getFormattedDate(todayDate.plusDays(1))

            var newTimestamp = todayDate.withHour(0).withMinute(0).withSecond(0)
            data[0].hourly?.forEach {
                it.timestamp = newTimestamp.toString()
                newTimestamp = newTimestamp.plusHours(1)
            }

            newTimestamp = todayDate.withHour(0).withMinute(0).withSecond(0).plusDays(1)
            data[1].hourly?.forEach {
                newTimestamp = newTimestamp.plusHours(1)
                it.timestamp = newTimestamp.toString()
            }
        }
        return response
    }

    override suspend fun setForecast(deviceId: String, forecast: List<WeatherData>) {
        // No-op
    }

    override suspend fun clear() {
        // No-op
    }
}

/**
 * In-memory user cache. Could be expanded to use SharedPreferences or a different cache.
 */
class CacheWeatherForecastDataSource : WeatherForecastDataSource {

    private var forecasts: ArrayMap<String, TimedForecastData> = ArrayMap()

    override suspend fun getForecast(
        deviceId: String,
        fromDate: String,
        toDate: String,
        exclude: String?
    ): Either<Failure, List<WeatherData>> {
        return forecasts[deviceId]?.let {
            if (it.isExpired()) Either.Left(DataError.CacheExpiredError)
            else Either.Right(it.value)
        } ?: Either.Left(DataError.CacheMissError)
    }

    override suspend fun setForecast(deviceId: String, forecast: List<WeatherData>) {
        this.forecasts[deviceId] = TimedForecastData(forecast)
    }

    override suspend fun clear() {
        this.forecasts.clear()
    }

    data class TimedForecastData(
        val value: List<WeatherData>,
        // Default cache expiration: 15 minutes
        private val cacheExpirationTime: Long = TimeUnit.MINUTES.toMillis(15)
    ) {
        private val creationTime: Long = now()

        fun isExpired() = (now() - creationTime) > cacheExpirationTime

        private fun now() = System.currentTimeMillis()
    }
}
