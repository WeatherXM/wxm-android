package com.weatherxm.data.datasource

import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.retrofitResponse
import com.weatherxm.TestUtils.testNetworkCall
import com.weatherxm.TestUtils.testThrowNotImplemented
import com.weatherxm.data.database.dao.BaseDao
import com.weatherxm.data.database.dao.DeviceHistoryDao
import com.weatherxm.data.database.entities.DeviceHourlyHistory
import com.weatherxm.data.models.DataError
import com.weatherxm.data.models.HourlyWeather
import com.weatherxm.data.models.WeatherData
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.ErrorResponse
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.ZonedDateTime

class WeatherHistoryDataSourceTest : BehaviorSpec({
    val apiService = mockk<ApiService>()
    val dao = mockk<DeviceHistoryDao>()
    val networkSource = NetworkWeatherHistoryDataSource(apiService)
    val databaseSource = DatabaseWeatherHistoryDataSource(dao)

    val deviceId = "deviceId"
    val fromDate = LocalDate.now().minusDays(1)
    val timestamp = ZonedDateTime.now()
    val toDate = LocalDate.now()
    val hourlyWeather = listOf(
        HourlyWeather(
            timestamp = timestamp,
            precipitation = 0F,
            precipAccumulated = 0F,
            temperature = 10F,
            feelsLike = 10F,
            windDirection = 0,
            humidity = 60,
            windSpeed = 5F,
            windGust = 0F,
            icon = null,
            precipProbability = 50,
            uvIndex = 0,
            cloudCover = null,
            pressure = 0F,
            dewPoint = 0F,
            solarIrradiance = 0F
        )
    )
    val databaseHourlyWeather =
        listOf(DeviceHourlyHistory.fromHourlyWeather(deviceId, hourlyWeather[0]))
    val weatherData = listOf(
        WeatherData(
            address = "",
            date = toDate,
            tz = "Europe/Athens",
            hourly = hourlyWeather,
            daily = null
        )
    )

    val weatherDataResponse = NetworkResponse.Success<List<WeatherData>, ErrorResponse>(
        weatherData,
        retrofitResponse(weatherData)
    )

    beforeSpec {
        justRun { BaseDao.Companion.DAOWrapper(dao).insertAllWithTimestamp(databaseHourlyWeather) }
    }

    context("Get historical data") {
        given("A Network and a Database Source providing the historical data") {
            When("Using the Network Source") {
                testNetworkCall(
                    "Historical Data",
                    hourlyWeather,
                    weatherDataResponse,
                    mockFunction = {
                        apiService.getWeatherHistory(
                            deviceId,
                            fromDate.toString(),
                            toDate.toString()
                        )
                    },
                    runFunction = { networkSource.getWeatherHistory(deviceId, fromDate, toDate) }
                )
            }
            When("Using the Database Source") {
                and("The Database is empty") {
                    every {
                        dao.getInRange(
                            deviceId = deviceId,
                            fromDate = fromDate.atStartOfDay().toString(),
                            toDate = toDate.plusDays(1).atStartOfDay().toString()
                        )
                    } returns emptyList()
                    then("return a DatabaseMissError") {
                        databaseSource.getWeatherHistory(deviceId, fromDate, toDate).leftOrNull()
                            .shouldBeTypeOf<DataError.DatabaseMissError>()
                    }
                }
                and("database is not empty") {
                    every {
                        dao.getInRange(
                            deviceId = deviceId,
                            fromDate = fromDate.atStartOfDay().toString(),
                            toDate = toDate.plusDays(1).atStartOfDay().toString()
                        )
                    } returns databaseHourlyWeather
                    then("return the historical data") {
                        databaseSource.getWeatherHistory(deviceId, fromDate, toDate)
                            .isSuccess(hourlyWeather)
                    }
                }
            }
        }
    }

    context("Set historical data") {
        given("A Network and a Database Source providing the SET mechanism") {
            When("Using the Network Source") {
                testThrowNotImplemented { networkSource.setWeatherHistory(deviceId, hourlyWeather) }
            }
            When("Using the Database Source") {
                then("save the historical data in the database") {
                    databaseSource.setWeatherHistory(deviceId, hourlyWeather)
                    verify(exactly = 1) {
                        BaseDao.Companion.DAOWrapper(dao)
                            .insertAllWithTimestamp(databaseHourlyWeather)
                    }
                }
            }
        }
    }
})
