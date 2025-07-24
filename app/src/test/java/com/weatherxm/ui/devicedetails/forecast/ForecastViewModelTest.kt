package com.weatherxm.ui.devicedetails.forecast

import com.weatherxm.R
import com.weatherxm.TestConfig.CONNECTION_TIMEOUT_MSG
import com.weatherxm.TestConfig.NO_CONNECTION_MSG
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.NetworkError.ConnectionTimeoutError
import com.weatherxm.data.models.NetworkError.NoConnectionError
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.usecases.ForecastUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ForecastViewModelTest : BehaviorSpec({
    val usecase = mockk<ForecastUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    val device = mockk<UIDevice>()
    lateinit var viewModel: ForecastViewModel

    val forecast = mockk<UIForecast>()

    val forecastGenericErrorMsg = "Fetching forecast failed"
    val invalidTimezoneMsg = "Invalid Timezone"
    val emptyForecastMsg = "Empty Forecast"
    val noConnectionFailure = NoConnectionError()
    val connectionTimeoutFailure = ConnectionTimeoutError()
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
            resources,
            usecase,
            analytics,
            dispatcher
        )
    }

    context("Get the rewards") {
        given("a usecase returning the rewards") {
            When("device is empty") {
                every { device.isEmpty() } returns true
                runTest { viewModel.fetchForecast() }
                then("Do nothing and return (check comment in ViewModel)") {
                    viewModel.onLoading().value shouldBe null
                    viewModel.onForecast().value shouldBe null
                    viewModel.onError().value shouldBe null
                }
                every { device.isEmpty() } returns false
            }
            When("flag isDeviceFromSearchResult = true indicating that we got here from search") {
                every { device.isDeviceFromSearchResult } returns true
                runTest { viewModel.fetchForecast() }
                then("Do nothing and return (check comment in ViewModel)") {
                    viewModel.onLoading().value shouldBe null
                    viewModel.onForecast().value shouldBe null
                    viewModel.onError().value shouldBe null
                }
                every { device.isDeviceFromSearchResult } returns false
            }
            When("device is unfollowed/public") {
                every { device.isUnfollowed() } returns true
                runTest { viewModel.fetchForecast() }
                then("Do nothing and return (check comment in ViewModel)") {
                    viewModel.onLoading().value shouldBe null
                    viewModel.onForecast().value shouldBe null
                    viewModel.onError().value shouldBe null
                }
                every { device.isUnfollowed() } returns false
            }
            When("usecase returns a failure") {
                and("it's a NoConnectionError failure") {
                    coMockEitherLeft({ usecase.getDeviceForecast(device, false) }, noConnectionFailure)
                    runTest { viewModel.fetchForecast() }
                    then("track the event's failure in the analytics") {
                        verify(exactly = 1) { analytics.trackEventFailure(any()) }
                    }
                    then("LiveData onError should post the UIError with a retry function") {
                        viewModel.onError().value?.errorMessage shouldBe NO_CONNECTION_MSG
                        viewModel.onError().value?.retryFunction shouldNotBe null
                    }
                }
                and("it's a ConnectionTimeoutError failure") {
                    coMockEitherLeft(
                        { usecase.getDeviceForecast(device, false) },
                        connectionTimeoutFailure
                    )
                    runTest { viewModel.fetchForecast() }
                    then("track the event's failure in the analytics") {
                        verify(exactly = 2) { analytics.trackEventFailure(any()) }
                    }
                    then("LiveData onError should post the UIError with a retry function") {
                        viewModel.onError().value?.errorMessage shouldBe CONNECTION_TIMEOUT_MSG
                        viewModel.onError().value?.retryFunction shouldNotBe null
                    }
                }
                and("it's an InvalidFromDate failure") {
                    coMockEitherLeft(
                        { usecase.getDeviceForecast(device, false) },
                        invalidFromDate
                    )
                    runTest { viewModel.fetchForecast() }
                    then("track the event's failure in the analytics") {
                        verify(exactly = 3) { analytics.trackEventFailure(any()) }
                    }
                    then("LiveData onError should post the UIError without a retry function") {
                        viewModel.onError().value?.errorMessage shouldBe forecastGenericErrorMsg
                        viewModel.onError().value?.retryFunction shouldBe null
                    }
                }
                and("it's an InvalidToDate failure") {
                    coMockEitherLeft(
                        { usecase.getDeviceForecast(device, false) },
                        invalidToDate
                    )
                    runTest { viewModel.fetchForecast() }
                    then("track the event's failure in the analytics") {
                        verify(exactly = 4) { analytics.trackEventFailure(any()) }
                    }
                    then("LiveData onError should post the UIError without a retry function") {
                        viewModel.onError().value?.errorMessage shouldBe forecastGenericErrorMsg
                        viewModel.onError().value?.retryFunction shouldBe null
                    }
                }
                and("it's an InvalidTimezone failure") {
                    coMockEitherLeft(
                        { usecase.getDeviceForecast(device, false) },
                        invalidTimezone
                    )
                    runTest { viewModel.fetchForecast() }
                    then("track the event's failure in the analytics") {
                        verify(exactly = 5) { analytics.trackEventFailure(any()) }
                    }
                    then("LiveData onError should post the UIError without a retry function") {
                        viewModel.onError().value?.errorMessage shouldBe invalidTimezoneMsg
                        viewModel.onError().value?.retryFunction shouldBe null
                    }
                }
                and("it's any other failure") {
                    coMockEitherLeft({ usecase.getDeviceForecast(device, true) }, failure)
                    runTest { viewModel.fetchForecast(true) }
                    then("track the event's failure in the analytics") {
                        verify(exactly = 6) { analytics.trackEventFailure(any()) }
                    }
                    then("LiveData onError should post a generic UIError") {
                        viewModel.onError().value?.errorMessage shouldBe REACH_OUT_MSG
                        viewModel.onError().value?.retryFunction shouldBe null
                    }
                }
            }
            When("usecase returns a success") {
                coMockEitherRight({ usecase.getDeviceForecast(device, false) }, forecast)
                and("the forecast is empty") {
                    every { forecast.isEmpty() } returns true
                    runTest { viewModel.fetchForecast() }
                    then("LiveData onError should post the UIError indicating an empty forecast") {
                        viewModel.onError().value?.errorMessage shouldBe emptyForecastMsg
                    }
                }
                then("LiveData onForecast should post the forecast we fetched") {
                    viewModel.onForecast().value shouldBe forecast
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
