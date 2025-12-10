package com.weatherxm.data.repository

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.datasource.CacheWeatherForecastDataSource
import com.weatherxm.data.datasource.NetworkWeatherForecastDataSource
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.WeatherData
import com.weatherxm.data.repository.WeatherForecastRepositoryImpl.Companion.PREFETCH_DAYS
import com.weatherxm.service.BillingService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.test.isRootTest
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.ZonedDateTime

class WeatherForecastRepositoryTest : BehaviorSpec({
    lateinit var networkSource: NetworkWeatherForecastDataSource
    lateinit var cacheSource: CacheWeatherForecastDataSource
    lateinit var repo: WeatherForecastRepositoryImpl
    lateinit var billingService: BillingService

    val location = Location.empty()
    val deviceId = "deviceId"
    val now = ZonedDateTime.now().toLocalDate()
    val fromDate = now.minusDays(PREFETCH_DAYS)
    val toDateLessThanPrefetched = fromDate.plusDays(PREFETCH_DAYS - 1)
    val forecastData = mockk<List<WeatherData>>()
    val purchaseToken = "purchaseToken"

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
                { networkSource.getDeviceForecast(deviceId, fromDate, now) },
                forecastData
            )
            coMockEitherRight(
                { cacheSource.getDeviceForecast(deviceId, fromDate, now) },
                forecastData
            )
            coMockEitherRight(
                { networkSource.getDeviceForecast(deviceId, fromDate, now, token = purchaseToken) },
                forecastData
            )
            coMockEitherRight({ networkSource.getLocationForecast(location) }, forecastData)
            coMockEitherRight({ cacheSource.getLocationForecast(location) }, forecastData)
            every { billingService.hasActiveSub() } returns false
            every { billingService.getActiveSubFlow().value?.purchaseToken } returns purchaseToken
        }
    }

    context("Handle force refresh in fetching forecast") {
        given("a force refresh value") {
            When("force refresh = FALSE") {
                then("clear cache should NOT be called") {
                    repo.getDeviceForecast(deviceId, fromDate, now, false)
                    coVerify(exactly = 0) { cacheSource.clearDeviceForecast() }
                }
            }
            When("force refresh = TRUE") {
                then("clear cache should be called") {
                    repo.getDeviceForecast(deviceId, fromDate, now, true)
                    coVerify(exactly = 1) { cacheSource.clearDeviceForecast() }
                }
            }
        }
    }

    context("Handle toDate in fetching forecast") {
        given("a toDate value") {
            When("is < than prefetch days ($PREFETCH_DAYS)") {
                then("the forecast fetched should be with a new toDate (including prefetch)") {
                    repo.getDeviceForecast(
                        deviceId,
                        fromDate,
                        toDateLessThanPrefetched,
                        false
                    ).isSuccess(forecastData)
                    coVerify(exactly = 1) { cacheSource.getDeviceForecast(deviceId, fromDate, now) }
                }
            }
            When("is >= than prefetch days") {
                then("the forecast fetched should be with the original toDate") {
                    repo.getDeviceForecast(deviceId, fromDate, now, false).isSuccess(forecastData)
                    coVerify(exactly = 2) { cacheSource.getDeviceForecast(deviceId, fromDate, now) }
                }
            }
        }
    }

    context("Handle cache in fetching device forecast") {
        given("if forecast data is in cache or not") {
            When("forecast data is in cache") {
                then("forecast should be fetched from cache") {
                    repo.getDeviceForecast(
                        deviceId, fromDate, now, false
                    ).isSuccess(forecastData)
                    coVerify(exactly = 1) { cacheSource.getDeviceForecast(deviceId, fromDate, now) }
                    coVerify(exactly = 0) {
                        networkSource.getDeviceForecast(
                            deviceId,
                            fromDate,
                            now
                        )
                    }
                }
            }
            When("forecast data is NOT in cache") {
                coMockEitherLeft(
                    { cacheSource.getDeviceForecast(deviceId, fromDate, now) },
                    failure
                )
                then("forecast should be fetched from network") {
                    repo.getDeviceForecast(deviceId, fromDate, now, false).isSuccess(forecastData)
                    coVerify(exactly = 1) {
                        networkSource.getDeviceForecast(
                            deviceId,
                            fromDate,
                            now
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
            every { billingService.hasActiveSub() } returns true
            When("the API returns the correct data") {
                then("forecast should be fetched from network") {
                    repo.getDeviceForecast(deviceId, fromDate, now, false).isSuccess(forecastData)
                }
            }
            When("the API returns a failure") {
                coMockEitherLeft(
                    {
                        networkSource.getDeviceForecast(
                            deviceId,
                            fromDate,
                            now,
                            token = purchaseToken
                        )
                    },
                    failure
                )
                then("forecast should return the failure") {
                    repo.getDeviceForecast(deviceId, fromDate, now, false).isError()
                }
            }
        }
    }
})
