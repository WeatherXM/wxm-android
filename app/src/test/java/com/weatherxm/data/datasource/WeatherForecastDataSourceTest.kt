package com.weatherxm.data.datasource

import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.TestConfig.cacheService
import com.weatherxm.TestUtils.retrofitResponse
import com.weatherxm.TestUtils.testGetFromCache
import com.weatherxm.TestUtils.testNetworkCall
import com.weatherxm.TestUtils.testThrowNotImplemented
import com.weatherxm.data.models.WeatherData
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.ErrorResponse
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.coJustRun
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class WeatherForecastDataSourceTest : BehaviorSpec({
    val apiService = mockk<ApiService>()
    val networkSource = NetworkWeatherForecastDataSource(apiService)
    val cacheSource = CacheWeatherForecastDataSource(cacheService)

    val deviceId = "deviceId"
    val fromDate = LocalDate.now()
    val toDate = LocalDate.now().plusDays(1)
    val forecastData = listOf<WeatherData>()

    val forecastResponse = NetworkResponse.Success<List<WeatherData>, ErrorResponse>(
        forecastData,
        retrofitResponse(forecastData)
    )

    beforeSpec {
        coJustRun { cacheService.setForecast(deviceId, forecastData) }
        coJustRun { cacheService.clearForecast() }
    }

    context("Get forecast") {
        given("A Network and a Cache Source providing the forecast") {
            When("Using the Network Source") {
                testNetworkCall(
                    "Forecast",
                    forecastData,
                    forecastResponse,
                    mockFunction = {
                        apiService.getForecast(deviceId, fromDate.toString(), toDate.toString())
                    },
                    runFunction = { networkSource.getDeviceForecast(deviceId, fromDate, toDate) }
                )
            }
            When("Using the Cache Source") {
                testGetFromCache(
                    "forecast",
                    forecastData,
                    mockFunction = { cacheService.getForecast(deviceId) },
                    runFunction = { cacheSource.getDeviceForecast(deviceId, fromDate, toDate) }
                )
            }
        }
    }

    context("Set forecast") {
        given("A Network and a Cache Source providing the SET mechanism") {
            When("Using the Network Source") {
                testThrowNotImplemented { networkSource.setDeviceForecast(deviceId, forecastData) }
            }
            When("Using the Cache Source") {
                then("save the forecast in cacheService") {
                    cacheSource.setDeviceForecast(deviceId, forecastData)
                    verify(exactly = 1) { cacheService.setForecast(deviceId, forecastData) }
                }
            }
        }
    }

    context("Clear forecast saved data") {
        given("A Network and a Cache Source providing the CLEAR mechanism") {
            When("Using the Network Source") {
                testThrowNotImplemented { networkSource.clear() }
            }
            When("Using the Cache Source") {
                then("clear the forecast in cacheService") {
                    cacheSource.clear()
                    verify(exactly = 1) { cacheService.clearForecast() }
                }
            }
        }
    }
})
