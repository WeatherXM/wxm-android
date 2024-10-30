package com.weatherxm.ui.rewardslist

import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.testHandleFailureViewModel
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.Reward
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.RewardTimelineType
import com.weatherxm.ui.common.TimelineReward
import com.weatherxm.ui.common.UIRewardsTimeline
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
class RewardsListViewModelTest : BehaviorSpec({
    val usecase = mockk<RewardsUseCase>()
    lateinit var analytics: AnalyticsWrapper
    lateinit var viewModel: RewardsListViewModel

    val emptyReward = Reward(null, null, null, null, null, null)
    val deviceId = "deviceId"
    val firstPage =
        UIRewardsTimeline(listOf(TimelineReward(RewardTimelineType.DATA, emptyReward)), true)
    val secondPage = UIRewardsTimeline(
        listOf(TimelineReward(RewardTimelineType.DATA, emptyReward)),
        hasNextPage = false
    )

    val firstPageRewards = firstPage.rewards
    val allRewards = mutableListOf<TimelineReward>().apply {
        addAll(firstPageRewards)
        addAll(secondPage.rewards)
    }
    val allRewardsWithEndOfList = allRewards.toMutableList().apply {
        add(TimelineReward(RewardTimelineType.END_OF_LIST, null))
    }

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
        justRun { analytics.trackEventFailure(any()) }

        viewModel = RewardsListViewModel(usecase, analytics)
    }

    context("Get the first page of the rewards") {
        given("A use case providing that first page") {
            When("it's a failure") {
                coMockEitherLeft({ usecase.getRewardsTimeline(deviceId, 0) }, failure)
                testHandleFailureViewModel(
                    { viewModel.fetchFirstPageRewards(deviceId) },
                    analytics,
                    viewModel.onFirstPageRewards(),
                    1,
                    REACH_OUT_MSG
                )
            }
            When("it's a success") {
                coMockEitherRight({ usecase.getRewardsTimeline(deviceId, 0) }, firstPage)
                runTest { viewModel.fetchFirstPageRewards(deviceId) }
                then("return the first page of the rewards") {
                    viewModel.onFirstPageRewards().isSuccess(firstPageRewards)
                }
            }
        }
    }

    context("Get a next page of the rewards") {
        given("A use case providing that next page") {
            When("it's a failure") {
                coMockEitherLeft({ usecase.getRewardsTimeline(deviceId, 1) }, failure)
                runTest { viewModel.fetchNewPageRewards(deviceId) }
                then("Do not send anything to the UI and log it in the analytics") {
                    verify(exactly = 2) { analytics.trackEventFailure(any()) }
                }
            }
            When("it's a success") {
                coMockEitherRight({ usecase.getRewardsTimeline(deviceId, 1) }, secondPage)
                runTest { viewModel.fetchNewPageRewards(deviceId) }
                then("return the second and the first page of the rewards") {
                    viewModel.onNewRewardsPage().isSuccess(allRewards)
                }
                and("Try to get another page (hasNextPage == false, there is no next page)") {
                    runTest { viewModel.fetchNewPageRewards(deviceId) }
                    then("Add an END_OF_LIST element at the rewards and post it with onEndOfData") {
                        viewModel.onEndOfData().value shouldBe allRewardsWithEndOfList
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
