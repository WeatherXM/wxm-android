package com.weatherxm.usecases

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.models.DailyData
import com.weatherxm.data.models.HourlyWeather
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.WeatherData
import com.weatherxm.data.repository.LocationsRepository
import com.weatherxm.data.repository.WeatherForecastRepository
import com.weatherxm.ui.common.LocationWeather
import com.weatherxm.util.Weather
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.time.ZoneId
import java.time.ZonedDateTime

class LocationsUseCaseTest : BehaviorSpec({
    val repository = mockk<LocationsRepository>()
    val forecastRepo = mockk<WeatherForecastRepository>()
    val usecase = LocationsUseCaseImpl(repository, forecastRepo)

    val location = Location.empty()
    val savedLocations = listOf(location)

    val utc = "UTC"
    val tomorrowInUtc = ZonedDateTime.now(ZoneId.of(utc)).plusDays(1).withHour(0)
    val weatherData = listOf(
        WeatherData(
            "Address",
            tomorrowInUtc.toLocalDate(),
            utc,
            listOf(
                HourlyWeather(
                    tomorrowInUtc,
                    null,
                    5F,
                    15F,
                    15F,
                    180,
                    75,
                    5F,
                    10F,
                    "clear-day",
                    0,
                    8,
                    null,
                    1010F,
                    5F,
                    400F
                ),
                HourlyWeather(
                    tomorrowInUtc.withHour(1),
                    null,
                    15F,
                    25F,
                    25F,
                    90,
                    65,
                    3F,
                    2F,
                    "clear-night",
                    0,
                    8,
                    null,
                    1010F,
                    5F,
                    400F
                )
            ),
            DailyData(
                tomorrowInUtc.toString(),
                10F,
                null,
                10F,
                25F,
                180,
                75,
                5F,
                10F,
                "clear-day",
                0,
                8,
                null,
                1010F
            )
        )
    )
    val locationWeather = LocationWeather(
        coordinates = location,
        address = "Address",
        icon = "clear-day",
        currentWeatherSummaryResId = Weather.getWeatherSummaryDesc("clear-day"),
        currentTemp = 15F,
        dailyMinTemp = 10F,
        dailyMaxTemp = 25F
    )

    beforeSpec {
        justRun { forecastRepo.clearLocationForecastFromCache() }
        every { repository.getSavedLocations() } returns savedLocations
        justRun { repository.addSavedLocation(Location.empty()) }
        justRun { repository.removeSavedLocation(Location.empty()) }
    }

    context("Get Location's Weather Forecast") {
        given("A repository providing the forecast data") {
            When("repository returns a failure") {
                coMockEitherLeft({ forecastRepo.getLocationForecast(location) }, failure)
                then("return that failure") {
                    usecase.getLocationWeather(location).isError()
                }
            }
            When("repository returns success along with the data") {
                coMockEitherRight({ forecastRepo.getLocationForecast(location) }, weatherData)
                then("return the respective LocationWeather") {
                    usecase.getLocationWeather(location).isSuccess(locationWeather)
                }
            }
        }
    }

    context("GET saved locations") {
        given("a datasource providing them") {
            then("return them") {
                usecase.getSavedLocations() shouldBe savedLocations
            }
        }
    }

    context("ADD / REMOVE the location from the saved ones") {
        When("ADD") {
            usecase.addSavedLocation(location)
            then("call the respective function in the usecase") {
                verify(exactly = 1) { repository.addSavedLocation(location) }
            }
        }
        When("REMOVE") {
            usecase.removeSavedLocation(location)
            then("call the respective function in the usecase") {
                verify(exactly = 1) { repository.removeSavedLocation(location) }
            }
        }
    }
})
