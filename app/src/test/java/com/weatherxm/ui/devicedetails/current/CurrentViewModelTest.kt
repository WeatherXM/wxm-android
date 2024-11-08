package com.weatherxm.ui.devicedetails.current

import com.weatherxm.TestConfig.CONNECTION_TIMEOUT_MSG
import com.weatherxm.TestConfig.DEVICE_NOT_FOUND_MSG
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
import com.weatherxm.usecases.DeviceDetailsUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class CurrentViewModelTest : BehaviorSpec({
    val usecase = mockk<DeviceDetailsUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    val device = mockk<UIDevice>()
    lateinit var viewModel: CurrentViewModel

    val returnedDevice = UIDevice.empty()
    val noConnectionFailure = NoConnectionError()
    val connectionTimeoutFailure = ConnectionTimeoutError()
    val deviceNotFoundFailure = ApiError.DeviceNotFound("")

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
        viewModel = CurrentViewModel(
            device,
            resources,
            usecase,
            analytics,
            dispatcher
        )
    }

    context("Get the device") {
        given("a usecase returning the device") {
            When("usecase returns a failure") {
                and("it's a NoConnectionError failure") {
                    coMockEitherLeft({ usecase.getDevice(device) }, noConnectionFailure)
                    runTest { viewModel.fetchDevice() }
                    then("track the event's failure in the analytics") {
                        verify(exactly = 1) { analytics.trackEventFailure(any()) }
                    }
                    then("LiveData onError should post the UIError with a retry function") {
                        viewModel.onError().value?.errorMessage shouldBe NO_CONNECTION_MSG
                        viewModel.onError().value?.retryFunction shouldNotBe null
                    }
                }
                and("it's a ConnectionTimeoutError failure") {
                    coMockEitherLeft({ usecase.getDevice(device) }, connectionTimeoutFailure)
                    runTest { viewModel.fetchDevice() }
                    then("track the event's failure in the analytics") {
                        verify(exactly = 2) { analytics.trackEventFailure(any()) }
                    }
                    then("LiveData onError should post the UIError with a retry function") {
                        viewModel.onError().value?.errorMessage shouldBe CONNECTION_TIMEOUT_MSG
                        viewModel.onError().value?.retryFunction shouldNotBe null
                    }
                }
                and("it's an DeviceNotFound failure") {
                    coMockEitherLeft({ usecase.getDevice(device) }, deviceNotFoundFailure)
                    runTest { viewModel.fetchDevice() }
                    then("track the event's failure in the analytics") {
                        verify(exactly = 3) { analytics.trackEventFailure(any()) }
                    }
                    then("LiveData onError should post the UIError without a retry function") {
                        viewModel.onError().value?.errorMessage shouldBe DEVICE_NOT_FOUND_MSG
                        viewModel.onError().value?.retryFunction shouldBe null
                    }
                }
                and("it's any other failure") {
                    coMockEitherLeft({ usecase.getDevice(device) }, failure)
                    runTest { viewModel.fetchDevice() }
                    then("track the event's failure in the analytics") {
                        verify(exactly = 4) { analytics.trackEventFailure(any()) }
                    }
                    then("LiveData onError should post a generic UIError") {
                        viewModel.onError().value?.errorMessage shouldBe REACH_OUT_MSG
                        viewModel.onError().value?.retryFunction shouldBe null
                    }
                }
            }
            When("usecase returns a success") {
                coMockEitherRight({ usecase.getDevice(device) }, returnedDevice)
                runTest { viewModel.fetchDevice() }
                then("LiveData onDevice should post the device we fetched") {
                    viewModel.onDevice().value shouldBe returnedDevice
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
