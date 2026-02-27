package com.weatherxm.ui.devicedetails.forecast

import com.weatherxm.R
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.service.BillingService
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.usecases.ForecastUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ForecastViewModelTest : BehaviorSpec({
    val forecastUseCase = mockk<ForecastUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    val billingService = mockk<BillingService>()
    val device = mockk<UIDevice>()
    lateinit var viewModel: ForecastViewModel

    val forecast = mockk<UIForecast>()

    val forecastGenericErrorMsg = "Fetching forecast failed"
    val invalidTimezoneMsg = "Invalid Timezone"
    val emptyForecastMsg = "Empty Forecast"
    val invalidFromDate = ApiError.UserError.InvalidFromDate("")
    val invalidToDate = ApiError.UserError.InvalidToDate("")
    val invalidTimezone = ApiError.UserError.InvalidTimezone("")

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
        every { billingService.hasActiveSub() } returns false
        every {
            resources.getString(R.string.forecast_empty)
        } returns emptyForecastMsg
        every {
            resources.getString(R.string.error_forecast_generic_message)
        } returns forecastGenericErrorMsg
        every {
            resources.getString(R.string.error_forecast_invalid_timezone)
        } returns invalidTimezoneMsg

        viewModel = ForecastViewModel(
            device,
            billingService,
            resources,
            forecastUseCase,
            analytics,
            dispatcher
        )
    }

    context("Get the forecast") {
        given("a usecase returning the forecast") {
            When("device is empty") {
                every { device.isEmpty() } returns true
                runTest { viewModel.fetchForecasts() }
                then("Do nothing and return (check comment in ViewModel)") {
                    viewModel.onDefaultForecast().value shouldBe null
                    viewModel.onPremiumForecast().value shouldBe null
                }
                every { device.isEmpty() } returns false
            }
            When("flag isDeviceFromSearchResult = true indicating that we got here from search") {
                every { device.isDeviceFromSearchResult } returns true
                runTest { viewModel.fetchForecasts() }
                then("Do nothing and return (check comment in ViewModel)") {
                    viewModel.onDefaultForecast().value shouldBe null
                    viewModel.onPremiumForecast().value shouldBe null
                }
                every { device.isDeviceFromSearchResult } returns false
            }
            When("device is unfollowed/public") {
                every { device.isUnfollowed() } returns true
                runTest { viewModel.fetchForecasts() }
                then("Do nothing and return (check comment in ViewModel)") {
                    viewModel.onDefaultForecast().value shouldBe null
                    viewModel.onPremiumForecast().value shouldBe null
                }
                every { device.isUnfollowed() } returns false
            }
            When("usecase returns a failure") {
                and("it's an InvalidFromDate failure") {
                    coMockEitherLeft(
                        { forecastUseCase.getDeviceDefaultForecast(device, false) },
                        invalidFromDate
                    )
                    runTest { viewModel.fetchForecasts() }
                    then("track the event's failure in the analytics") {
                        verify(exactly = 1) { analytics.trackEventFailure(any()) }
                    }
                    then("onDefaultForecast should post the error without a retry function") {
                        viewModel.onDefaultForecast().isError(forecastGenericErrorMsg)
                    }
                }
                and("it's an InvalidToDate failure") {
                    coMockEitherLeft(
                        { forecastUseCase.getDeviceDefaultForecast(device, false) },
                        invalidToDate
                    )
                    runTest { viewModel.fetchForecasts() }
                    then("track the event's failure in the analytics") {
                        verify(exactly = 2) { analytics.trackEventFailure(any()) }
                    }
                    then("onDefaultForecast should post the error without a retry function") {
                        viewModel.onDefaultForecast().isError(forecastGenericErrorMsg)
                    }
                }
                and("it's an InvalidTimezone failure") {
                    coMockEitherLeft(
                        { forecastUseCase.getDeviceDefaultForecast(device, false) },
                        invalidTimezone
                    )
                    runTest { viewModel.fetchForecasts() }
                    then("track the event's failure in the analytics") {
                        verify(exactly = 3) { analytics.trackEventFailure(any()) }
                    }
                    then("onDefaultForecast should post the error without a retry function") {
                        viewModel.onDefaultForecast().isError(invalidTimezoneMsg)
                    }
                }
                and("it's any other failure") {
                    coMockEitherLeft(
                        { forecastUseCase.getDeviceDefaultForecast(device, true) },
                        failure
                    )
                    runTest { viewModel.fetchForecasts(true) }
                    then("track the event's failure in the analytics") {
                        verify(exactly = 4) { analytics.trackEventFailure(any()) }
                    }
                    then("LiveData onDefaultForecast should post a generic error") {
                        viewModel.onDefaultForecast().isError(REACH_OUT_MSG)
                    }
                }
            }
            When("usecase returns a success") {
                coMockEitherRight(
                    { forecastUseCase.getDeviceDefaultForecast(device, false) },
                    forecast
                )
                and("the forecast is empty") {
                    every { forecast.isEmpty() } returns true
                    runTest { viewModel.fetchForecasts() }
                    then("onDefaultForecast should post the error indicating an empty forecast") {
                        viewModel.onDefaultForecast().isError(emptyForecastMsg)
                    }
                }
                then("LiveData onDefaultForecast should post the forecast we fetched") {
                    every { forecast.isEmpty() } returns false
                    runTest { viewModel.fetchForecasts() }
                    viewModel.onDefaultForecast().isSuccess(forecast)
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
