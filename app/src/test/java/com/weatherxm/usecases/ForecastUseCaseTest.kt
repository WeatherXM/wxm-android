package com.weatherxm.usecases

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.ApiError
import com.weatherxm.data.DailyData
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.WeatherData
import com.weatherxm.data.repository.WeatherForecastRepository
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.ui.common.UIForecastDay
import com.weatherxm.ui.common.empty
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import java.time.ZoneId
import java.time.ZonedDateTime

class ForecastUseCaseTest : BehaviorSpec({
    val repo = mockk<WeatherForecastRepository>()
    val usecase = ForecastUseCaseImpl(repo)

    val device = UIDevice.empty()
    val forceRefresh = false
    val utc = "UTC"
    val tomorrowInUtc = ZonedDateTime.now(ZoneId.of(utc)).plusDays(1)
    val weatherData = listOf(
        WeatherData(
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

    val hourlyWeather = HourlyWeather(
        timestamp = tomorrowInUtc,
        precipitation = null,
        precipAccumulated = 5F,
        temperature = 15F,
        feelsLike = 15F,
        windDirection = 180,
        humidity = 75,
        windSpeed = 5F,
        windGust = 10F,
        icon = "clear-day",
        precipProbability = 0,
        uvIndex = 8,
        cloudCover = null,
        pressure = 1010F,
        dewPoint = 5F,
        solarIrradiance = 400F
    )
    val uiForecast = UIForecast(
        next24Hours = listOf(hourlyWeather),
        forecastDays = listOf(
            UIForecastDay(
                date = tomorrowInUtc.toLocalDate(),
                icon = "clear-day",
                minTemp = 10F,
                maxTemp = 25F,
                precipProbability = 0,
                precip = 10F,
                windSpeed = 5F,
                windDirection = 180,
                humidity = 75,
                pressure = 1010F,
                uv = 8,
                hourlyWeather = listOf(hourlyWeather)
            )
        )
    )

    context("Get Weather Forecast") {
        given("A repository providing the forecast data") {
            When("Device has a null timezone property") {
                then("return INVALID_TIMEZONE failure") {
                    val failure = usecase.getForecast(device, forceRefresh).leftOrNull()
                    (failure is ApiError.UserError.InvalidTimezone) shouldBe true
                }
            }
            When("Device does has an empty timezone property") {
                device.timezone = String.empty()
                then("return INVALID_TIMEZONE failure") {
                    val failure = usecase.getForecast(device, forceRefresh).leftOrNull()
                    (failure is ApiError.UserError.InvalidTimezone) shouldBe true
                }
            }
            When("Device has a valid timezone property") {
                device.timezone = utc
                val fromDate = ZonedDateTime.now(ZoneId.of(device.timezone)).toLocalDate()
                val toDate = fromDate.plusDays(7)
                and("repository returns a failure") {
                    coMockEitherLeft({
                        repo.getDeviceForecast(device.id, fromDate, toDate, forceRefresh)
                    }, failure)
                    then("return that failure") {
                        usecase.getForecast(device, forceRefresh).isError()
                    }
                }
                When("repository returns success along with the data") {
                    coMockEitherRight({
                        repo.getDeviceForecast(device.id, fromDate, toDate, forceRefresh)
                    }, weatherData)
                    then("return the respective UIForecast") {
                        usecase.getForecast(device, forceRefresh).isSuccess(uiForecast)
                    }
                }
            }
        }
    }
})
