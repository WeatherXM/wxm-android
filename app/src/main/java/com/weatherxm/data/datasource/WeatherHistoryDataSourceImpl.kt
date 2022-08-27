package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.database.dao.BaseDao
import com.weatherxm.data.database.dao.DeviceHistoryDao
import com.weatherxm.data.database.entities.DeviceHourlyHistory
import com.weatherxm.data.map
import com.weatherxm.data.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

class NetworkWeatherHistoryDataSource(private val apiService: ApiService) :
    WeatherHistoryDataSource {

    override suspend fun getWeatherHistory(
        deviceId: String,
        fromDate: String,
        toDate: String,
        exclude: String?
    ): Either<Failure, List<HourlyWeather>> {
        return apiService.getWeatherHistory(deviceId, fromDate, toDate, exclude).map()
            .map { response ->
                response
                    .mapNotNull { it.hourly }
                    .flatten()
            }
    }

    override suspend fun setWeatherHistory(deviceId: String, weatherData: List<HourlyWeather>) {
        // No-op
    }
}

class DatabaseWeatherHistoryDataSource(private val deviceHistoryDao: DeviceHistoryDao) :
    WeatherHistoryDataSource {

    override suspend fun getWeatherHistory(
        deviceId: String,
        fromDate: String,
        toDate: String,
        exclude: String?
    ): Either<Failure, List<HourlyWeather>> {
        return Either.Right(deviceHistoryDao.getInRange(deviceId, fromDate, toDate).map {
            HourlyWeather(
                timestamp = it.timestamp.toString(),
                precipitation = it.precipitationIntensity,
                temperature = it.temperature,
                feelsLike = it.feelsLike,
                windDirection = it.windDirection,
                humidity = it.humidity,
                windSpeed = it.windSpeed,
                windGust = it.windGust,
                icon = null,
                precipProbability = it.precipProbability,
                uvIndex = it.uvIndex,
                cloudCover = null,
                pressure = it.pressure,
            )
        })
    }

    override suspend fun setWeatherHistory(deviceId: String, weatherData: List<HourlyWeather>) {
        CoroutineScope(coroutineContext).launch(Dispatchers.IO) {
            weatherData
                .map { DeviceHourlyHistory.fromHourlyWeather(deviceId, it) }
                .apply {
                    if (isNotEmpty()) {
                        BaseDao.Companion.DAOWrapper(deviceHistoryDao).insertAllWithTimestamp(this)
                    }
                }
        }
    }
}
