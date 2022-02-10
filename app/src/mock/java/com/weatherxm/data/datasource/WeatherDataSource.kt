package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.WeatherData
import com.weatherxm.data.map
import com.weatherxm.data.network.ApiService
import com.weatherxm.util.getFormattedDate
import java.time.ZoneId
import java.time.ZonedDateTime

interface WeatherDataSource {
    suspend fun getForecast(
        deviceId: String,
        fromDate: String,
        toDate: String,
        exclude: String?
    ): Either<Failure, List<WeatherData>>

    suspend fun getWeatherHistory(
        deviceId: String,
        fromDate: String,
        toDate: String,
        exclude: String?
    ): Either<Failure, List<WeatherData>>
}

class WeatherDataSourceImpl(
    private val apiService: ApiService
) : WeatherDataSource {

    // Custom fix to change the timestamps to today/tomorrow.
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

            data[0].date = getFormattedDate(todayDate.toString())
            data[1].date = getFormattedDate(todayDate.plusDays(1).toString())

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

    override suspend fun getWeatherHistory(
        deviceId: String,
        fromDate: String,
        toDate: String,
        exclude: String?
    ): Either<Failure, List<WeatherData>> {
        return apiService.getWeatherHistory(deviceId, fromDate, toDate, exclude).map()
    }
}
