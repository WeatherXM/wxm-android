package com.weatherxm.data.datasource

import androidx.collection.ArrayMap
import arrow.core.Either
import com.weatherxm.data.DataError
import com.weatherxm.data.Failure
import com.weatherxm.data.WeatherData
import com.weatherxm.data.map
import com.weatherxm.data.network.ApiService
import java.util.concurrent.TimeUnit

interface WeatherDataSource {
    suspend fun getForecast(
        deviceId: String,
        fromDate: String,
        toDate: String,
        exclude: String? = null
    ): Either<Failure, List<WeatherData>>

    suspend fun setForecast(deviceId: String, forecast: List<WeatherData>)

    suspend fun clear()

    suspend fun getWeatherHistory(
        deviceId: String,
        fromDate: String,
        toDate: String,
        exclude: String?
    ): Either<Failure, List<WeatherData>>
}

class NetworkWeatherDataSource(private val apiService: ApiService) : WeatherDataSource {

    override suspend fun getForecast(
        deviceId: String,
        fromDate: String,
        toDate: String,
        exclude: String?
    ): Either<Failure, List<WeatherData>> {
        return apiService.getForecast(deviceId, fromDate, toDate, exclude).map()
    }

    override suspend fun setForecast(deviceId: String, forecast: List<WeatherData>) {
        // No-op
    }

    override suspend fun clear() {
        // No-op
    }

    override suspend fun getWeatherHistory(
        deviceId: String,
        fromDate: String,
        toDate: String,
        exclude: String?
    ): Either<Failure, List<WeatherData>> {
        return apiService.getWeatherHistory(deviceId, fromDate, toDate, exclude).map()
    }
}

/**
 * In-memory user cache. Could be expanded to use SharedPreferences or a different cache.
 */
class CacheWeatherDataSource : WeatherDataSource {

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

    override suspend fun getWeatherHistory(
        deviceId: String,
        fromDate: String,
        toDate: String,
        exclude: String?
    ): Either<Failure, List<WeatherData>> {
        TODO("Not yet implemented. Probably not using cache after all on History.")
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
