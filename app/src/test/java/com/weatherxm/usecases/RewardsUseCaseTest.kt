package com.weatherxm.usecases

import android.icu.text.CompactDecimalFormat
import android.icu.text.NumberFormat
import com.weatherxm.TestConfig.context
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.models.BoostReward
import com.weatherxm.data.models.BoostRewardDetails
import com.weatherxm.data.models.BoostRewardMetadata
import com.weatherxm.data.models.BoostRewardResponse
import com.weatherxm.data.models.Reward
import com.weatherxm.data.models.RewardDetails
import com.weatherxm.data.models.RewardsTimeline
import com.weatherxm.data.repository.RewardsRepository
import com.weatherxm.ui.common.BoostDetailInfo
import com.weatherxm.ui.common.RewardTimelineType
import com.weatherxm.ui.common.TimelineReward
import com.weatherxm.ui.common.UIBoost
import com.weatherxm.ui.common.UIRewardsTimeline
import com.weatherxm.ui.common.empty
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.NumberUtils
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.time.ZonedDateTime

class RewardsUseCaseTest : BehaviorSpec({
    val repository = mockk<RewardsRepository>()
    val usecase = RewardsUseCaseImpl(repository, context)

    val deviceId = "deviceId"
    val page = 0
    val timestamp = ZonedDateTime.now()
    val formattedDate = timestamp.getFormattedDate(true)
    val emptyReward = Reward(timestamp, null, null, null, null, null)
    val validReward = Reward(timestamp, 10F, 0F, 10F, 100, null)

    val emptyTimeline = RewardsTimeline(emptyList(), 1, false)
    val emptyUITimeline = UIRewardsTimeline(emptyList(), reachedTotal = true)

    val timeline = RewardsTimeline(listOf(emptyReward, validReward), 1, false)
    val uiTimeline =
        UIRewardsTimeline(listOf(TimelineReward(RewardTimelineType.DATA, validReward)), false)

    val rewardDetails = mockk<RewardDetails>()

    val boostCode = "beta_rewards"
    val boostReward = BoostReward(boostCode, null, null, null, null, 10F, 100, 10F)
    val emptyBoostReward = BoostReward(null, null, null, null, null, null, null, null)
    val boostRewardsResponse = BoostRewardResponse(
        boostCode,
        BoostRewardMetadata("title", "about", "imgUrl", "docUrl", "about"),
        BoostRewardDetails(10, 10F, 10F, timestamp, timestamp, timestamp, timestamp)
    )
    val emptyBoostRewardResponse = BoostRewardResponse(null, null, null)
    val uiBoost = UIBoost(
        title = "title",
        actualReward = "10.00",
        boostScore = 100,
        lostRewards = "0.00",
        boostDesc = "Boost details description",
        about = "about",
        docUrl = "docUrl",
        imgUrl = "imgUrl",
        details = listOf(
            BoostDetailInfo("Rewardable station-hours", "10"),
            BoostDetailInfo("Daily tokens to be rewarded (max)", "10 \$WXM"),
            BoostDetailInfo("Total tokens to be rewarded (max)", "10 \$WXM"),
            BoostDetailInfo("Boost Period", "$formattedDate - $formattedDate")
        )
    )
    val emptyUiBoost =
        UIBoost(
            String.empty(),
            "0.00",
            null,
            "0.00",
            String.empty(),
            String.empty(),
            String.empty(),
            String.empty(),
            emptyList()
        )

    beforeSpec {
        startKoin {
            modules(
                module {
                    single<CompactDecimalFormat> {
                        mockk<CompactDecimalFormat>()
                    }
                    single<NumberFormat> {
                        mockk<NumberFormat>()
                    }
                }
            )
        }
        every { NumberUtils.compactNumber(any()) } returns "10"
        every { NumberUtils.formatNumber(any()) } returns "10"
    }

    context("Get timeline of rewards") {
        given("A repository providing the timeline") {
            When("it's a success") {
                and("Data are empty") {
                    then("return an empty timeline") {
                        coMockEitherRight(
                            { repository.getRewardsTimeline(deviceId, page) },
                            emptyTimeline
                        )
                        usecase.getRewardsTimeline(deviceId, page).isSuccess(emptyUITimeline)
                    }
                }
                and("Data are not empty") {
                    then("return the UIRewardsTimeline") {
                        coMockEitherRight(
                            { repository.getRewardsTimeline(deviceId, page) },
                            timeline
                        )
                        usecase.getRewardsTimeline(deviceId, page).isSuccess(uiTimeline)
                    }
                }
            }
            When("it's a failure") {
                then("return the failure") {
                    coMockEitherLeft({ repository.getRewardsTimeline(deviceId, page) }, failure)
                    usecase.getRewardsTimeline(deviceId, page).isError()
                }
            }
        }
    }

    context("Get reward details") {
        given("A repository providing the reward details") {
            When("it's a success") {
                then("return the RewardDetails") {
                    coMockEitherRight(
                        { repository.getRewardDetails(deviceId, timestamp) },
                        rewardDetails
                    )
                    usecase.getRewardDetails(deviceId, timestamp).isSuccess(rewardDetails)
                }
            }
            When("it's a failure") {
                then("return the failure") {
                    coMockEitherLeft({ repository.getRewardDetails(deviceId, timestamp) }, failure)
                    usecase.getRewardDetails(deviceId, timestamp).isError()
                }
            }
        }
    }

    context("Get boost reward") {
        given("A repository providing the boost reward") {
            When("it's a success") {
                and("it's a known BoostReward") {
                    coMockEitherRight(
                        { repository.getBoostReward(deviceId, boostCode) },
                        boostRewardsResponse
                    )
                    then("return the UIBoost") {
                        usecase.getBoostReward(deviceId, boostReward).isSuccess(uiBoost)
                    }
                }
                and("it's an empty BoostReward") {
                    coMockEitherRight(
                        { repository.getBoostReward(deviceId, String.empty()) },
                        emptyBoostRewardResponse
                    )
                    then("return an empty UIBoost") {
                        usecase.getBoostReward(deviceId, emptyBoostReward).isSuccess(emptyUiBoost)
                    }
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ repository.getBoostReward(deviceId, boostCode) }, failure)
                then("return the failure") {
                    usecase.getBoostReward(deviceId, boostReward).isError()
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
