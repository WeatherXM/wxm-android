package com.weatherxm.data.datasource

import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.TestConfig.cacheService
import com.weatherxm.TestUtils.retrofitResponse
import com.weatherxm.TestUtils.testGetFromCache
import com.weatherxm.TestUtils.testNetworkCall
import com.weatherxm.TestUtils.testThrowNotImplemented
import com.weatherxm.data.locationToText
import com.weatherxm.data.models.Location
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
    val location = Location.empty()

    val forecastResponse = NetworkResponse.Success<List<WeatherData>, ErrorResponse>(
        forecastData,
        retrofitResponse(forecastData)
    )

    beforeSpec {
        coJustRun { cacheService.setDeviceForecast(deviceId, forecastData) }
        coJustRun { cacheService.clearDeviceForecast() }
        coJustRun { cacheService.setLocationForecast(location.locationToText(), forecastData) }
        coJustRun { cacheService.clearLocationForecast() }
    }

    context("Get device forecast") {
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
                    mockFunction = { cacheService.getDeviceForecast(deviceId) },
                    runFunction = { cacheSource.getDeviceForecast(deviceId, fromDate, toDate) }
                )
            }
        }
    }

    context("Set device forecast") {
        given("A Network and a Cache Source providing the SET mechanism") {
            When("Using the Network Source") {
                testThrowNotImplemented { networkSource.setDeviceForecast(deviceId, forecastData) }
            }
            When("Using the Cache Source") {
                then("save the forecast in cacheService") {
                    cacheSource.setDeviceForecast(deviceId, forecastData)
                    verify(exactly = 1) { cacheService.setDeviceForecast(deviceId, forecastData) }
                }
            }
        }
    }

    context("Clear device forecast saved data") {
        given("A Network and a Cache Source providing the CLEAR mechanism") {
            When("Using the Network Source") {
                testThrowNotImplemented { networkSource.clearDeviceForecast() }
            }
            When("Using the Cache Source") {
                then("clear the forecast in cacheService") {
                    cacheSource.clearDeviceForecast()
                    verify(exactly = 1) { cacheService.clearDeviceForecast() }
                }
            }
        }
    }

    context("Get location forecast") {
        given("A Network and a Cache Source providing the forecast") {
            When("Using the Network Source") {
                testNetworkCall(
                    "Forecast",
                    forecastData,
                    forecastResponse,
                    mockFunction = { apiService.getLocationForecast(location.lat, location.lon) },
                    runFunction = { networkSource.getLocationForecast(location) }
                )
            }
            When("Using the Cache Source") {
                testGetFromCache(
                    "forecast",
                    forecastData,
                    mockFunction = { cacheService.getLocationForecast(location.locationToText()) },
                    runFunction = { cacheSource.getLocationForecast(location) }
                )
            }
        }
    }

    context("Set location forecast") {
        given("A Network and a Cache Source providing the SET mechanism") {
            When("Using the Network Source") {
                testThrowNotImplemented {
                    networkSource.setLocationForecast(
                        location,
                        forecastData
                    )
                }
            }
            When("Using the Cache Source") {
                then("save the forecast in cacheService") {
                    cacheSource.setLocationForecast(location, forecastData)
                    verify(exactly = 1) {
                        cacheService.setLocationForecast(location.locationToText(), forecastData)
                    }
                }
            }
        }
    }

    context("Clear location forecast saved data") {
        given("A Network and a Cache Source providing the CLEAR mechanism") {
            When("Using the Network Source") {
                testThrowNotImplemented { networkSource.clearLocationForecast() }
            }
            When("Using the Cache Source") {
                then("clear the forecast in cacheService") {
                    cacheSource.clearLocationForecast()
                    verify(exactly = 1) { cacheService.clearLocationForecast() }
                }
            }
        }
    }
})
