package com.weatherxm.ui.devicedetails.rewards

import com.weatherxm.TestConfig.CONNECTION_TIMEOUT_MSG
import com.weatherxm.TestConfig.NO_CONNECTION_MSG
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.NetworkError.ConnectionTimeoutError
import com.weatherxm.data.models.NetworkError.NoConnectionError
import com.weatherxm.data.models.Rewards
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.DeviceDetailsUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class RewardsViewModelTest : BehaviorSpec({
    val usecase = mockk<DeviceDetailsUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    val device = mockk<UIDevice>()
    lateinit var viewModel: RewardsViewModel

    val deviceId = "deviceId"
    val rewards = mockk<Rewards>()

    val noConnectionFailure = NoConnectionError()
    val connectionTimeoutFailure = ConnectionTimeoutError()

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
        every { device.id } returns deviceId

        viewModel = RewardsViewModel(
            device,
            resources,
            usecase,
            analytics
        )
    }

    context("Get the rewards") {
        given("a usecase returning the rewards") {
            When("device is empty") {
                every { device.isEmpty() } returns true
                runTest { viewModel.fetchRewardsFromNetwork() }
                then("Do nothing and return (check comment in ViewModel)") {
                    viewModel.onLoading().value shouldBe null
                    viewModel.onRewards().value shouldBe null
                    viewModel.onError().value shouldBe null
                }
            }
            When("the device is not empty") {
                every { device.isEmpty() } returns false
                When("usecase returns a failure") {
                    and("it's a NoConnectionError failure") {
                        coMockEitherLeft({ usecase.getRewards(deviceId) }, noConnectionFailure)
                        runTest { viewModel.fetchRewardsFromNetwork() }
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
                            { usecase.getRewards(deviceId) },
                            connectionTimeoutFailure
                        )
                        runTest { viewModel.fetchRewardsFromNetwork() }
                        then("track the event's failure in the analytics") {
                            verify(exactly = 2) { analytics.trackEventFailure(any()) }
                        }
                        then("LiveData onError should post the UIError with a retry function") {
                            viewModel.onError().value?.errorMessage shouldBe CONNECTION_TIMEOUT_MSG
                            viewModel.onError().value?.retryFunction shouldNotBe null
                        }
                    }
                    and("it's any other failure") {
                        coMockEitherLeft({ usecase.getRewards(deviceId) }, failure)
                        runTest { viewModel.fetchRewardsFromNetwork() }
                        then("track the event's failure in the analytics") {
                            verify(exactly = 3) { analytics.trackEventFailure(any()) }
                        }
                        then("LiveData onError should post a generic UIError") {
                            viewModel.onError().value?.errorMessage shouldBe REACH_OUT_MSG
                            viewModel.onError().value?.retryFunction shouldBe null
                        }
                    }
                }
                When("usecase returns a success") {
                    coMockEitherRight({ usecase.getRewards(deviceId) }, rewards)
                    runTest { viewModel.fetchRewardsFromNetwork() }
                    then("LiveData onRewards should post the rewards object just fetched") {
                        viewModel.onRewards().value shouldBe rewards
                        viewModel.onLoading().value shouldBe false
                    }
                }
            }
        }
    }

    afterSpec {
        Dispatchers.resetMain()
        stopKoin()
    }
})
