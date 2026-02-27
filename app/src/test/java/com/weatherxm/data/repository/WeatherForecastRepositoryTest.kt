package com.weatherxm.data.repository

import com.android.billingclient.api.Purchase
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.datasource.CacheWeatherForecastDataSource
import com.weatherxm.data.datasource.NetworkWeatherForecastDataSource
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.WeatherData
import com.weatherxm.service.BillingService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.test.isRootTest
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.mockito.ArgumentMatchers.any
import java.time.LocalDate

class WeatherForecastRepositoryTest : BehaviorSpec({
    lateinit var networkSource: NetworkWeatherForecastDataSource
    lateinit var cacheSource: CacheWeatherForecastDataSource
    lateinit var repo: WeatherForecastRepositoryImpl
    lateinit var billingService: BillingService

    val location = Location.empty()
    val deviceId = "deviceId"
    val fromDate = LocalDate.now()
    val toDate = fromDate.plusDays(7)
    val forecastData = mockk<List<WeatherData>>()
    val purchaseToken = "purchaseToken"
    val purchaseFlow = MutableStateFlow<Purchase?>(null)

    beforeInvocation { testCase, _ ->
        if (testCase.isRootTest()) {
            networkSource = mockk<NetworkWeatherForecastDataSource>()
            cacheSource = mockk<CacheWeatherForecastDataSource>()
            billingService = mockk<BillingService>()
            repo = WeatherForecastRepositoryImpl(billingService, networkSource, cacheSource)
            coJustRun { cacheSource.clearDeviceForecast() }
            coJustRun { cacheSource.clearLocationForecast() }
            coJustRun { cacheSource.setDeviceForecast(deviceId, forecastData) }
            coJustRun { cacheSource.setLocationForecast(location, forecastData) }
            coMockEitherRight(
                {
                    networkSource.getDeviceDefaultForecast(
                        deviceId,
                        fromDate,
                        toDate,
                        token = any()
                    )
                },
                forecastData
            )
            coMockEitherRight(
                { cacheSource.getDeviceDefaultForecast(deviceId, fromDate, toDate) },
                forecastData
            )
            coMockEitherRight({ networkSource.getLocationForecast(location) }, forecastData)
            coMockEitherRight({ cacheSource.getLocationForecast(location) }, forecastData)
            every { billingService.getActiveSubFlow() } returns purchaseFlow
        }
    }

    context("Handle force refresh in fetching forecast") {
        given("a force refresh value") {
            When("force refresh = FALSE") {
                then("clear cache should NOT be called") {
                    repo.getDeviceDefaultForecast(deviceId, fromDate, toDate, false)
                    coVerify(exactly = 0) { cacheSource.clearDeviceForecast() }
                }
            }
            When("force refresh = TRUE") {
                then("clear cache should be called") {
                    repo.getDeviceDefaultForecast(deviceId, fromDate, toDate, true)
                    coVerify(exactly = 1) { cacheSource.clearDeviceForecast() }
                }
            }
        }
    }

    context("Handle cache in fetching device default forecast") {
        given("if forecast data is in cache or not") {
            When("forecast data is in cache") {
                then("forecast should be fetched from cache") {
                    repo.getDeviceDefaultForecast(deviceId, fromDate, toDate, false)
                        .isSuccess(forecastData)
                    coVerify(exactly = 1) {
                        cacheSource.getDeviceDefaultForecast(
                            deviceId,
                            fromDate,
                            toDate
                        )
                    }
                    coVerify(exactly = 0) {
                        networkSource.getDeviceDefaultForecast(
                            deviceId,
                            fromDate,
                            toDate,
                            token = any()
                        )
                    }
                }
            }
            When("forecast data is NOT in cache") {
                coMockEitherLeft(
                    { cacheSource.getDeviceDefaultForecast(deviceId, fromDate, toDate) },
                    failure
                )
                then("forecast should be fetched from network") {
                    repo.getDeviceDefaultForecast(deviceId, fromDate, toDate, false)
                        .isSuccess(forecastData)
                    coVerify(exactly = 1) {
                        networkSource.getDeviceDefaultForecast(
                            deviceId,
                            fromDate,
                            toDate,
                            token = any()
                        )
                    }
                }
                then("forecast should be saved in cache") {
                    coVerify(exactly = 1) { cacheSource.setDeviceForecast(deviceId, forecastData) }
                }
            }
        }
    }

    given("requesting to clear location cache") {
        Then("cache should be cleared") {
            repo.clearLocationForecastFromCache()
            coVerify(exactly = 1) { cacheSource.clearLocationForecast() }
        }
    }

    context("Handle cache in fetching location forecast") {
        given("if forecast data is in cache or not") {
            When("forecast data is in cache") {
                then("forecast should be fetched from cache") {
                    repo.getLocationForecast(location).isSuccess(forecastData)
                    coVerify(exactly = 1) { cacheSource.getLocationForecast(location) }
                    coVerify(exactly = 0) { networkSource.getLocationForecast(location) }
                }
            }
            When("forecast data is NOT in cache") {
                coMockEitherLeft({ cacheSource.getLocationForecast(location) }, failure)
                then("forecast should be fetched from network") {
                    repo.getLocationForecast(location).isSuccess(forecastData)
                    coVerify(exactly = 1) { networkSource.getLocationForecast(location) }
                }
                then("forecast should be saved in cache") {
                    coVerify(exactly = 1) {
                        cacheSource.setLocationForecast(location, forecastData)
                    }
                }
            }
        }
    }

    context("Handle fetching premium forecast") {
        given("the datasource that we use to perform the API call") {
            val purchase = mockk<Purchase>()
            every { purchase.purchaseToken } returns purchaseToken
            val activePurchaseFlow = MutableStateFlow<Purchase?>(purchase)
            every { billingService.getActiveSubFlow() } returns activePurchaseFlow

            When("the API returns the correct data") {
                then("forecast should be fetched from network") {
//                    repo.getDevicePremiumForecast(deviceId, fromDate, toDate)
//                        .isSuccess(forecastData)
//                    coVerify(exactly = 1) {
//                        networkSource.getDevicePremiumForecast(
//                            deviceId,
//                            fromDate,
//                            toDate,
//                            token = purchaseToken
//                        )
//                    }
                }
            }
            When("the API returns a failure") {
                coMockEitherLeft(
                    {
                        networkSource.getDevicePremiumForecast(
                            deviceId,
                            fromDate,
                            toDate,
                            token = purchaseToken
                        )
                    },
                    failure
                )
                then("forecast should return the failure") {
                    repo.getDevicePremiumForecast(deviceId, fromDate, toDate).isError()
                }
            }
        }
    }
})
