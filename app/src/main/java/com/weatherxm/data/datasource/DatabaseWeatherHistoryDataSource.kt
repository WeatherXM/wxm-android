package com.weatherxm.data.datasource

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.weatherxm.data.models.DataError
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.HourlyWeather
import com.weatherxm.data.database.dao.BaseDao
import com.weatherxm.data.database.dao.DeviceHistoryDao
import com.weatherxm.data.database.entities.DeviceHourlyHistory
import java.time.LocalDate

class DatabaseWeatherHistoryDataSource(
    private val dao: DeviceHistoryDao
) : WeatherHistoryDataSource {
    override suspend fun getWeatherHistory(
        deviceId: String,
        fromDate: LocalDate,
        toDate: LocalDate
    ): Either<Failure, List<HourlyWeather>> {
        return dao
            .getInRange(
                deviceId = deviceId,
                fromDate = fromDate.atStartOfDay().toString(),
                toDate = toDate.plusDays(1).atStartOfDay().toString()
            )
            .map {
                HourlyWeather(
                    timestamp = it.timestamp,
                    precipitation = it.precipitationIntensity,
                    precipAccumulated = it.precipAccumulated,
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
                    dewPoint = it.dewPoint,
                    solarIrradiance = it.solarIrradiance
                )
            }
            .right()
            // Return DatabaseMissError if list is empty
            .flatMap { b ->
                b.takeIf { it.isNotEmpty() }?.right() ?: DataError.DatabaseMissError.left()
            }
    }

    override suspend fun setWeatherHistory(deviceId: String, data: List<HourlyWeather>) {
        data
            .map { DeviceHourlyHistory.fromHourlyWeather(deviceId, it) }
            .apply {
                BaseDao.Companion.DAOWrapper(dao).insertAllWithTimestamp(this)
            }
    }
}
