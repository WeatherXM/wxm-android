package com.weatherxm.ui.forecastdetails

import com.weatherxm.R
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.testHandleFailureViewModel
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.HourlyWeather
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.Charts
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.ui.common.UIForecastDay
import com.weatherxm.ui.common.UILocation
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.ChartsUseCase
import com.weatherxm.usecases.ForecastUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ForecastDetailsViewModelTest : BehaviorSpec({
    val forecastUseCase = mockk<ForecastUseCase>()
    val chartsUseCase = mockk<ChartsUseCase>()
    val authUseCase = mockk<AuthUseCase>()
    val device = UIDevice.empty()
    val location = UILocation.empty()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: ForecastDetailsViewModel

    val today = LocalDate.now()
    val tomorrow = LocalDate.now().plusDays(1)
    val hourlyWeather = HourlyWeather(
        timestamp = ZonedDateTime.of(today, LocalTime.of(0, 0, 0), ZoneId.systemDefault()),
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
    val hourlyWeather7am = HourlyWeather(
        timestamp = ZonedDateTime.of(today, LocalTime.of(7, 0, 0), ZoneId.systemDefault()),
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
    val forecastDay = UIForecastDay(
        date = today,
        null,
        5F,
        10F,
        0,
        0F,
        5F,
        0,
        50,
        1000F,
        0,
        listOf(hourlyWeather)
    )
    val forecastDayTomorrow = UIForecastDay(
        date = tomorrow,
        null,
        5F,
        10F,
        0,
        0F,
        5F,
        0,
        50,
        1000F,
        0,
        null
    )
    val emptyForecast: UIForecast = UIForecast.empty()
    val forecast =
        UIForecast(device.address, listOf(hourlyWeather), listOf(forecastDay, forecastDayTomorrow))
    val charts = mockk<Charts>()

    val invalidFromDate = ApiError.UserError.InvalidFromDate("")
    val invalidToDate = ApiError.UserError.InvalidToDate("")
    val invalidTimezone = ApiError.UserError.InvalidTimezone("")

    val emptyForecastMsg = "Empty Forecast"
    val forecastGenericErrorMsg = "Fetching forecast failed"
    val invalidTimezoneMsg = "Invalid Timezone"

    listener(InstantExecutorListener())

    beforeSpec {
        startKoin {
            modules(
                module {
                    single<Resources> {
                        resources
                    }
                }
            )
        }
        justRun { analytics.trackEventFailure(any()) }
        every { charts.date } returns today
        every {
            resources.getString(R.string.forecast_empty)
        } returns emptyForecastMsg
        every {
            resources.getString(R.string.error_forecast_generic_message)
        } returns forecastGenericErrorMsg
        every {
            resources.getString(R.string.error_forecast_invalid_timezone)
        } returns invalidTimezoneMsg
        every { chartsUseCase.createHourlyCharts(today, any()) } returns charts
        every { chartsUseCase.createHourlyCharts(tomorrow, any()) } returns charts

        viewModel = ForecastDetailsViewModel(
            device,
            location,
            resources,
            analytics,
            authUseCase,
            chartsUseCase,
            forecastUseCase,
            dispatcher
        )
    }

    context("Get weather forecast of this device") {
        given("a usecase returning the forecast") {
            When("it's a failure") {
                and("it's an InvalidFromDate failure") {
                    coMockEitherLeft(
                        { forecastUseCase.getDeviceForecast(device) },
                        invalidFromDate
                    )
                    testHandleFailureViewModel(
                        { viewModel.fetchDeviceForecast() },
                        analytics,
                        viewModel.onForecastLoaded(),
                        1,
                        forecastGenericErrorMsg
                    )
                }
                and("it's an InvalidToDate failure") {
                    coMockEitherLeft(
                        { forecastUseCase.getDeviceForecast(device) },
                        invalidToDate
                    )
                    testHandleFailureViewModel(
                        { viewModel.fetchDeviceForecast() },
                        analytics,
                        viewModel.onForecastLoaded(),
                        2,
                        forecastGenericErrorMsg
                    )
                }
                and("it's an InvalidTimezone failure") {
                    coMockEitherLeft(
                        { forecastUseCase.getDeviceForecast(device) },
                        invalidTimezone
                    )
                    testHandleFailureViewModel(
                        { viewModel.fetchDeviceForecast() },
                        analytics,
                        viewModel.onForecastLoaded(),
                        3,
                        invalidTimezoneMsg
                    )
                }
                and("it's any other failure") {
                    coMockEitherLeft(
                        { forecastUseCase.getDeviceForecast(device) },
                        failure
                    )
                    testHandleFailureViewModel(
                        { viewModel.fetchDeviceForecast() },
                        analytics,
                        viewModel.onForecastLoaded(),
                        4,
                        REACH_OUT_MSG
                    )
                }
                then("forecast should be set to empty") {
                    viewModel.forecast().isEmpty() shouldBe true
                }
            }
            When("it's a success") {
                and("an empty forecast returned") {
                    coMockEitherRight(
                        { forecastUseCase.getDeviceForecast(device) },
                        emptyForecast
                    )
                    runTest { viewModel.fetchDeviceForecast() }
                    then("LiveData onForecastLoaded should post the error for the empty forecast") {
                        viewModel.onForecastLoaded().isError(emptyForecastMsg)
                    }
                    then("forecast should be set to empty") {
                        viewModel.forecast().isEmpty() shouldBe true
                    }
                }
                and("a valid non-empty forecast is returned") {
                    coMockEitherRight(
                        { forecastUseCase.getDeviceForecast(device) },
                        forecast
                    )
                    runTest { viewModel.fetchDeviceForecast() }
                    then("LiveData onForecastLoaded should post Unit as a success value") {
                        viewModel.onForecastLoaded().isSuccess(Unit)
                    }
                    then("forecast should be set to the returned value") {
                        viewModel.forecast() shouldBe forecast
                    }
                }
            }
        }
    }

    context("Get the position in the forecast of the selected day") {
        given("a selected day as a LocalDate ISO String") {
            When("it's null") {
                then("return 0") {
                    viewModel.getSelectedDayPosition(null) shouldBe 0
                }
            }
            When("it's a date we don't have in the forecast") {
                then("return 0") {
                    viewModel.getSelectedDayPosition(LocalDate.MIN.toString()) shouldBe 0
                }
            }
            When("it's a date we do have in the forecast") {
                then("return the position") {
                    viewModel.getSelectedDayPosition(tomorrow.toString()) shouldBe 1
                }
            }
        }
    }

    context("Get the default (starting) hourly weather position in the forecast") {
        given("a list of hourly weather") {
            When("the list contains the 7am hourly weather") {
                val hourlies = listOf(hourlyWeather, hourlyWeather7am)
                then("return the position of the 7am hourly weather") {
                    viewModel.getDefaultHourPosition(hourlies) shouldBe 1
                }
            }
            When("the list does not contain the 7am hourly weather") {
                then("return the position of the first hourly weather") {
                    viewModel.getDefaultHourPosition(listOf(hourlyWeather)) shouldBe 0
                }
            }
        }
    }

    context("Get charts for forecast") {
        given("the usecase returning the charts") {
            then("return the charts") {
                viewModel.getCharts(forecastDay) shouldBe charts
                viewModel.getCharts(forecastDayTomorrow) shouldBe charts
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
