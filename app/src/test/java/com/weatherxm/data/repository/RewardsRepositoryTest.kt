package com.weatherxm.data.repository

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.BoostRewardResponse
import com.weatherxm.data.RewardDetails
import com.weatherxm.data.Rewards
import com.weatherxm.data.RewardsTimeline
import com.weatherxm.data.WalletRewards
import com.weatherxm.data.datasource.RewardsDataSource
import com.weatherxm.data.repository.RewardsRepositoryImpl.Companion.TIMELINE_MINUS_MONTHS_TO_FETCH
import com.weatherxm.util.toISODate
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk
import java.time.ZoneId
import java.time.ZonedDateTime

class RewardsRepositoryTest : BehaviorSpec({
    val dataSource = mockk<RewardsDataSource>()
    val repository = RewardsRepositoryImpl(dataSource)

    val deviceId = "deviceId"
    val boostCode = "boostCode"
    val walletAddress = "walletAddress"
    val now = ZonedDateTime.now()
    val timezone = ZoneId.of("UTC").toString()
    val fromDate = ZonedDateTime.now().minusMonths(TIMELINE_MINUS_MONTHS_TO_FETCH).toISODate()
    val dateISO = now.toISODate()
    val rewards = mockk<Rewards>()
    val rewardDetails = mockk<RewardDetails>()
    val walletRewards = mockk<WalletRewards>()
    val rewardsTimeline = mockk<RewardsTimeline>()
    val boostReward = mockk<BoostRewardResponse>()


    given("A device id") {
        and("requesting rewards timeline") {
            When("it's a success") {
                then("return the rewards timeline") {
                    coMockEitherRight({
                        dataSource.getRewardsTimeline(
                            deviceId, 0, timezone = timezone, fromDate = fromDate
                        )
                    }, rewardsTimeline)
                    repository.getRewardsTimeline(deviceId, 0).isSuccess(rewardsTimeline)
                }
            }
            When("it's a failure") {
                then("return the failure") {
                    coMockEitherLeft({
                        dataSource.getRewardsTimeline(
                            deviceId, 0, timezone = timezone, fromDate = fromDate
                        )
                    }, failure)
                    repository.getRewardsTimeline(deviceId, 0).isError()
                }
            }
        }
        and("requesting rewards") {
            When("it's a success") {
                then("return the rewards") {
                    coMockEitherRight({ dataSource.getRewards(deviceId) }, rewards)
                    repository.getRewards(deviceId).isSuccess(rewards)
                }
            }
            When("it's a failure") {
                then("return the failure") {
                    coMockEitherLeft({ dataSource.getRewards(deviceId) }, failure)
                    repository.getRewards(deviceId).isError()
                }
            }
        }
        and("a date") {
            and("requesting reward details") {
                When("it's a success") {
                    then("return the reward details") {
                        coMockEitherRight(
                            { dataSource.getRewardDetails(deviceId, dateISO) },
                            rewardDetails
                        )
                        repository.getRewardDetails(deviceId, now).isSuccess(rewardDetails)
                    }
                }
                When("it's a failure") {
                    then("return the failure") {
                        coMockEitherLeft(
                            { dataSource.getRewardDetails(deviceId, dateISO) },
                            failure
                        )
                        repository.getRewardDetails(deviceId, now).isError()
                    }
                }
            }
        }
        and("a boost code") {
            and("requesting boost reward") {
                When("it's a success") {
                    then("return the boost reward") {
                        coMockEitherRight(
                            { dataSource.getBoostReward(deviceId, boostCode) },
                            boostReward
                        )
                        repository.getBoostReward(deviceId, boostCode).isSuccess(boostReward)
                    }
                }
                When("it's a failure") {
                    then("return the failure") {
                        coMockEitherLeft(
                            { dataSource.getBoostReward(deviceId, boostCode) },
                            failure
                        )
                        repository.getBoostReward(deviceId, boostCode).isError()
                    }
                }
            }
        }
    }
    given("A wallet address") {
        and("requesting wallet rewards") {
            When("it's a success") {
                then("return the wallet rewards") {
                    coMockEitherRight(
                        { dataSource.getWalletRewards(walletAddress) },
                        walletRewards
                    )
                    repository.getWalletRewards(walletAddress).isSuccess(walletRewards)
                }
            }
            When("it's a failure") {
                then("return the failure") {
                    coMockEitherLeft({ dataSource.getWalletRewards(walletAddress) }, failure)
                    repository.getWalletRewards(walletAddress).isError()
                }
            }
        }
    }
})
