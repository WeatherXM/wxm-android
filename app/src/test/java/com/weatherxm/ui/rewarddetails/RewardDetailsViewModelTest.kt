package com.weatherxm.ui.rewarddetails

import com.weatherxm.TestConfig.DEVICE_NOT_FOUND_MSG
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.testHandleFailureViewModel
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.RewardDetails
import com.weatherxm.data.models.RewardSplit
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.RewardSplitsData
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.RewardsUseCase
import com.weatherxm.usecases.UserUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.time.ZonedDateTime

class RewardDetailsViewModelTest : BehaviorSpec({
    val rewardsUseCase = mockk<RewardsUseCase>()
    val userUseCase = mockk<UserUseCase>()
    val authUseCase = mockk<AuthUseCase>()
    val device = UIDevice.empty()
    lateinit var analytics: AnalyticsWrapper
    lateinit var viewModel: RewardDetailsViewModel

    val walletAddress = "walletAddress"
    val timestamp = ZonedDateTime.now()
    val rewardSplits = listOf(
        RewardSplit(walletAddress, 50, 50F),
        RewardSplit("", 50, 50F)
    )
    val rewardDetails = RewardDetails(
        timestamp,
        null,
        null,
        null,
        null,
        rewardSplits
    )
    val rewardSplitData = RewardSplitsData(rewardSplits, walletAddress)
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
        analytics = mockk<AnalyticsWrapper>()
        justRun { analytics.trackEventFailure(any()) }
        coMockEitherRight({ authUseCase.isLoggedIn() }, true)
        coMockEitherRight({ userUseCase.getWalletAddress() }, walletAddress)

        viewModel = RewardDetailsViewModel(
            device,
            analytics,
            resources,
            rewardsUseCase,
            userUseCase,
            authUseCase,
            dispatcher
        )
    }

    context("Get the reward details") {
        given("A use case providing these reward details") {
            When("it's a failure") {
                and("It's a DeviceNotFound failure") {
                    coMockEitherLeft(
                        { rewardsUseCase.getRewardDetails(device.id, timestamp) },
                        deviceNotFoundFailure
                    )
                    testHandleFailureViewModel(
                        { viewModel.fetchRewardDetails(timestamp) },
                        analytics,
                        viewModel.onRewardDetails(),
                        1,
                        DEVICE_NOT_FOUND_MSG
                    )
                }
                and("it's any other failure") {
                    coMockEitherLeft(
                        { rewardsUseCase.getRewardDetails(device.id, timestamp) },
                        failure
                    )
                    testHandleFailureViewModel(
                        { viewModel.fetchRewardDetails(timestamp) },
                        analytics,
                        viewModel.onRewardDetails(),
                        2,
                        REACH_OUT_MSG
                    )
                }
            }
            When("it's a success") {
                coMockEitherRight(
                    { rewardsUseCase.getRewardDetails(device.id, timestamp) },
                    rewardDetails
                )
                runTest { viewModel.fetchRewardDetails(timestamp) }
                then("return the reward details") {
                    viewModel.onRewardDetails().isSuccess(rewardDetails)
                }
                then("get reward splits data") {
                    viewModel.getRewardSplitsData {
                        it shouldBe rewardSplitData
                    }
                }
                then("get if the user is a stakeholder or not") {
                    viewModel.isStakeHolder() shouldBe true
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
