package com.weatherxm.ui.devicesrewards

import com.weatherxm.R
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.testHandleFailureViewModel
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.repository.RewardsRepositoryImpl.Companion.RewardsSummaryMode
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.DeviceTotalRewards
import com.weatherxm.ui.common.DeviceTotalRewardsDetails
import com.weatherxm.ui.common.DevicesRewards
import com.weatherxm.ui.common.DevicesRewardsByRange
import com.weatherxm.ui.common.Status
import com.weatherxm.usecases.RewardsUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
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
class DevicesRewardsViewModelTest : BehaviorSpec({
    lateinit var usecase: RewardsUseCase
    lateinit var analytics: AnalyticsWrapper
    lateinit var viewModel: DevicesRewardsViewModel

    val devicesRewardsByRange = mockk<DevicesRewardsByRange>()
    val erroneousDetails = DeviceTotalRewardsDetails.empty().apply {
        this.status = Status.ERROR
    }
    val deviceId = "deviceId"
    val deviceTotalRewardDetails = DeviceTotalRewardsDetails.empty()
    val rewards = DevicesRewards(
        0F,
        0F,
        listOf(DeviceTotalRewards("", "", 0F, deviceTotalRewardDetails))
    )

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
        analytics = mockk<AnalyticsWrapper>()
        usecase = mockk<RewardsUseCase>()
        justRun { analytics.trackEventFailure(any()) }

        viewModel = DevicesRewardsViewModel(rewards, usecase, analytics)
    }

    context("Get Total Rewards by range for owned devices") {
        given("a usecase providing the total rewards by range for the owned devices") {
            When("it's a failure") {
                coMockEitherLeft(
                    { usecase.getDevicesRewardsByRange(RewardsSummaryMode.YEAR) },
                    failure
                )
                testHandleFailureViewModel(
                    { viewModel.getDevicesRewardsByRangeTotals(R.id.year) },
                    analytics,
                    viewModel.onRewardsByRange(),
                    1,
                    REACH_OUT_MSG
                )
            }
            When("it's a success") {
                coMockEitherRight(
                    { usecase.getDevicesRewardsByRange(RewardsSummaryMode.WEEK) },
                    devicesRewardsByRange
                )
                runTest { viewModel.getDevicesRewardsByRangeTotals() }
                then("LiveData OnRewardsByRange should post the the rewards we just fetched") {
                    viewModel.onRewardsByRange().isSuccess(devicesRewardsByRange)
                }
            }
        }
    }

    context("Get Total Device Rewards by range") {
        given("a usecase providing the total device rewards by range") {
            When("it's a failure") {
                coMockEitherLeft(
                    { usecase.getDeviceRewardsByRange(deviceId, RewardsSummaryMode.MONTH) },
                    failure
                )
                erroneousDetails.mode = RewardsSummaryMode.MONTH
                runTest { viewModel.getDeviceRewardsByRange(deviceId, 0, R.id.month) }
                then("the rewards object in the ViewModel should be updated") {
                    viewModel.rewards.devices[0].details shouldBe erroneousDetails
                }
                then("LiveData onDeviceRewardDetails should post the error") {
                    viewModel.onDeviceRewardDetails().value shouldBe Pair(0, erroneousDetails)
                }
                then("track the event's failure in the analytics") {
                    verify(exactly = 2) { analytics.trackEventFailure(any()) }
                }
            }
            When("it's a success") {
                coMockEitherRight(
                    { usecase.getDeviceRewardsByRange(deviceId, RewardsSummaryMode.WEEK) },
                    deviceTotalRewardDetails
                )
                runTest { viewModel.getDeviceRewardsByRange(deviceId, 0) }
                then("the rewards object in the ViewModel should be updated") {
                    viewModel.rewards.devices[0].details shouldBe deviceTotalRewardDetails
                }
                then("LiveData onDeviceRewardDetails should post the error") {
                    viewModel.onDeviceRewardDetails().value shouldBe Pair(
                        0,
                        deviceTotalRewardDetails
                    )
                }
            }
        }
        given("a request to cancel the fetching") {
            then("cancel the fetching") {
                viewModel.cancelFetching(0)
            }
        }
    }

    afterSpec {
        Dispatchers.resetMain()
        stopKoin()
    }
})
