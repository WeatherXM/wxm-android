package com.weatherxm.data.repository

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.datasource.DatabaseWeatherHistoryDataSource
import com.weatherxm.data.datasource.NetworkWeatherHistoryDataSource
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.scopes.BehaviorSpecWhenContainerScope
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.ZonedDateTime

class WeatherHistoryRepositoryTest : BehaviorSpec({
    lateinit var networkSource: NetworkWeatherHistoryDataSource
    lateinit var dbSource: DatabaseWeatherHistoryDataSource
    lateinit var repo: WeatherHistoryRepositoryImpl

    val deviceId = "deviceId"
    val currentZone = ZonedDateTime.now().zone
    val today = LocalDate.now()
    val data = mutableListOf<HourlyWeather>()
    val hourlyWeather = mockk<HourlyWeather>() {
        every { timestamp } returns ZonedDateTime.now()
    }

    beforeContainer {
        networkSource = mockk<NetworkWeatherHistoryDataSource>()
        dbSource = mockk<DatabaseWeatherHistoryDataSource>()
        repo = WeatherHistoryRepositoryImpl(networkSource, dbSource)
        coJustRun { dbSource.setWeatherHistory(deviceId, data) }
    }

    suspend fun BehaviorSpecWhenContainerScope.verifyNetworkCallAfterDatabaseMiss() {
        coMockEitherRight({ dbSource.getWeatherHistory(deviceId, today, today) }, data)
        then("proceed with a network call to fetch the data") {
            coMockEitherRight({ networkSource.getWeatherHistory(deviceId, today, today) }, data)
            repo.getHourlyWeatherHistory(deviceId, today).isSuccess(data)
            coVerify(exactly = 1) {
                networkSource.getWeatherHistory(deviceId, today, today)
            }
        }
    }

    suspend fun BehaviorSpecWhenContainerScope.testFetchDataFromNetwork() {
        When("the network call fails") {
            coMockEitherLeft({ networkSource.getWeatherHistory(deviceId, today, today) }, failure)
            then("return the failure") {
                repo.getHourlyWeatherHistory(deviceId, today, true).isError()
            }
        }
        When("network call succeeds & data are empty") {
            coMockEitherRight({ networkSource.getWeatherHistory(deviceId, today, today) }, data)
            then("return the data") {
                repo.getHourlyWeatherHistory(deviceId, today, true).isSuccess(data)
            }
            then("do NOT save the data in the DB (as these are empty)") {
                coVerify(exactly = 0) { dbSource.setWeatherHistory(deviceId, data) }
            }
        }
        When("network call succeeds & data are NOT empty") {
            data.add(hourlyWeather)
            coMockEitherRight({ networkSource.getWeatherHistory(deviceId, today, today) }, data)
            then("return the data") {
                repo.getHourlyWeatherHistory(deviceId, today, true).isSuccess(data)
            }
            then("save the data in database") {
                coVerify(exactly = 1) { dbSource.setWeatherHistory(deviceId, data) }
            }
        }
    }

    suspend fun BehaviorSpecWhenContainerScope.testFetchDataFromDatabaseFirst() {
        When("fetching data from database fails") {
            coMockEitherLeft({ dbSource.getWeatherHistory(deviceId, today, today) }, failure)
            then("proceed with a network call to fetch the data") {
                coMockEitherRight({ networkSource.getWeatherHistory(deviceId, today, today) }, data)
                repo.getHourlyWeatherHistory(deviceId, today).isSuccess(data)
                coVerify(exactly = 1) { networkSource.getWeatherHistory(deviceId, today, today) }
            }
        }
        When("fetching data from database succeeds") {
            and("but data are NOT complete (EMPTY)") {
                data.clear()
                verifyNetworkCallAfterDatabaseMiss()
            }
            and("but data are NOT complete (LESS THAN 24 ELEMENTS)") {
                data.add(hourlyWeather)
                verifyNetworkCallAfterDatabaseMiss()
            }
            and("but data are NOT complete (FIRST ENTRY IS TOO LATE)") {
                data.clear()
                data.add(mockk<HourlyWeather> {
                    every { timestamp } returns
                        today.atStartOfDay().plusHours(3).atZone(currentZone)
                })
                repeat(23) {
                    data.add(hourlyWeather)
                }
                verifyNetworkCallAfterDatabaseMiss()
            }
            and("but data are NOT complete (LAST ENTRY IS TOO EARLY)") {
                data.clear()
                data.add(mockk<HourlyWeather> {
                    every { timestamp } returns today.atStartOfDay(currentZone)
                })
                repeat(22) {
                    data.add(hourlyWeather)
                }
                data.add(mockk<HourlyWeather> {
                    every { timestamp } returns today
                        .plusDays(1)
                        .atStartOfDay(currentZone)
                        .minusNanos(1)
                        .minusHours(3)
                })
                verifyNetworkCallAfterDatabaseMiss()
            }
            and("data are complete") {
                data.clear()
                data.add(mockk<HourlyWeather> {
                    every { timestamp } returns today.atStartOfDay(currentZone)
                })
                repeat(22) {
                    data.add(hourlyWeather)
                }
                data.add(mockk<HourlyWeather> {
                    every { timestamp } returns today
                        .plusDays(1)
                        .atStartOfDay(currentZone)
                        .minusNanos(1)
                })
                coMockEitherRight({ dbSource.getWeatherHistory(deviceId, today, today) }, data)
                then("return that data from the database") {
                    repo.getHourlyWeatherHistory(deviceId, today).isSuccess(data)
                    coVerify(exactly = 0) {
                        networkSource.getWeatherHistory(deviceId, today, today)
                    }
                }
            }
        }
    }

    context("Get Weather History data") {
        given("a Force Update flag") {
            When("the flag is TRUE") {
                and("the data should be fetched from network") {
                    testFetchDataFromNetwork()
                }
            }
            When("the flag is FALSE") {
                and("the data should be fetched from the database first") {
                    testFetchDataFromDatabaseFirst()
                }
            }
        }
    }
})
