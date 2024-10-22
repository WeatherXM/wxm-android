package com.weatherxm.ui.rewardboost

import com.weatherxm.TestConfig.DEVICE_NOT_FOUND_MSG
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.BoostReward
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.UIBoost
import com.weatherxm.ui.rewardboosts.RewardBoostViewModel
import com.weatherxm.usecases.RewardsUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
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
class RewardBoostViewModelTest : BehaviorSpec({
    val usecase = mockk<RewardsUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    val deviceId = "deviceId"
    val viewModel = RewardBoostViewModel(deviceId, analytics, resources, usecase)
    val boostReward = mockk<BoostReward>()
    val uiBoost = mockk<UIBoost>()
    val deviceNotFoundFailure = ApiError.DeviceNotFound("")

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
    }

    context("Get boost reward") {
        given("A use case providing the boost reward") {
            When("it's a success") {
                coMockEitherRight({ usecase.getBoostReward(deviceId, boostReward) }, uiBoost)
                then("LiveData posts a success") {
                    runTest { viewModel.fetchRewardBoost(boostReward) }
                    viewModel.onBoostReward().isSuccess(uiBoost)
                }
            }
            When("it's a failure") {
                and("It's a DeviceNotFound failure") {
                    coMockEitherLeft(
                        { usecase.getBoostReward(deviceId, boostReward) },
                        deviceNotFoundFailure
                    )
                    runTest { viewModel.fetchRewardBoost(boostReward) }
                    then("Log that error as a failure event") {
                        verify(exactly = 1) { analytics.trackEventFailure(any()) }
                    }
                    then("LiveData posts an error with a specific DeviceNotFound message") {
                        viewModel.onBoostReward().isError(DEVICE_NOT_FOUND_MSG)
                    }
                }
                and("it's any other failure") {
                    coMockEitherLeft({ usecase.getBoostReward(deviceId, boostReward) }, failure)
                    runTest { viewModel.fetchRewardBoost(boostReward) }
                    then("Log that error as a failure event") {
                        verify(exactly = 2) { analytics.trackEventFailure(any()) }
                    }
                    then("LiveData posts an error") {
                        viewModel.onBoostReward().isError(REACH_OUT_MSG)
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
