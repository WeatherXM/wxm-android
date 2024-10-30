package com.weatherxm.ui.devicehistory

import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.testHandleFailureViewModel
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.HourlyWeather
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.Charts
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.ChartsUseCase
import com.weatherxm.usecases.HistoryUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest : BehaviorSpec({
    val historyUseCase = mockk<HistoryUseCase>()
    val chartsUseCase = mockk<ChartsUseCase>()
    val device = UIDevice.empty()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: HistoryViewModel

    val date = LocalDate.now()
    val historyWeather = listOf<HourlyWeather>()
    val charts = mockk<Charts>()

    val invalidFromDate = ApiError.UserError.InvalidFromDate("")
    val invalidToDate = ApiError.UserError.InvalidToDate("")

    val fetchingHistoryFailed = "Fetching history failed"

    listener(InstantExecutorListener())
    Dispatchers.setMain(StandardTestDispatcher())

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
        justRun {
            analytics.trackEventSelectContent(
                AnalyticsService.ParamValue.HISTORY_DAY.paramValue,
                Pair(FirebaseAnalytics.Param.ITEM_ID, device.id),
                Pair(AnalyticsService.CustomParam.DATE.paramName, date.toString())
            )
        }
        every { charts.date } returns date
        every {
            resources.getString(R.string.error_history_generic_message)
        } returns fetchingHistoryFailed
        every { chartsUseCase.createHourlyCharts(date, any()) } returns charts

        viewModel = HistoryViewModel(
            device,
            historyUseCase,
            chartsUseCase,
            resources,
            analytics
        )
    }

    context("Get weather history of this device for a date") {
        given("a date selected") {
            When("the usecase returns the history successfully") {
                coMockEitherRight(
                    { historyUseCase.getWeatherHistory(device, date, true) },
                    historyWeather
                )
            }
            runTest { viewModel.selectNewDate(date) }
            then("get the current date selected") {
                viewModel.getCurrentDateShown() shouldBe date
            }
            then("check if this date is Today") {
                viewModel.isTodayShown() shouldBe true
            }
            then("LiveData onNewDate should post the new date") {
                viewModel.onNewDate().value shouldBe date
            }
            then("from the response we create the Charts and use charts LiveData to post them") {
                viewModel.charts().isSuccess(charts)
            }
            When("the usecase returns a failure") {
                and("it's an InvalidFromDate failure") {
                    coMockEitherLeft(
                        { historyUseCase.getWeatherHistory(device, date, false) },
                        invalidFromDate
                    )
                    testHandleFailureViewModel(
                        { viewModel.fetchWeatherHistory() },
                        analytics,
                        viewModel.charts(),
                        1,
                        fetchingHistoryFailed
                    )
                }
                and("it's an InvalidToDate failure") {
                    coMockEitherLeft(
                        { historyUseCase.getWeatherHistory(device, date, false) },
                        invalidToDate
                    )
                    testHandleFailureViewModel(
                        { viewModel.fetchWeatherHistory() },
                        analytics,
                        viewModel.charts(),
                        2,
                        fetchingHistoryFailed
                    )
                }
                and("it's any other failure") {
                    coMockEitherLeft(
                        { historyUseCase.getWeatherHistory(device, date, false) },
                        failure
                    )
                    testHandleFailureViewModel(
                        { viewModel.fetchWeatherHistory() },
                        analytics,
                        viewModel.charts(),
                        3,
                        REACH_OUT_MSG
                    )
                }
            }
        }
    }

    afterSpec {
        Dispatchers.resetMain()
        stopKoin()
    }
})
