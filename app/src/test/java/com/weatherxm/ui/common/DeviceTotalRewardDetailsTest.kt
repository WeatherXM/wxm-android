package com.weatherxm.ui.common

import com.weatherxm.data.repository.RewardsRepositoryImpl
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class DeviceTotalRewardDetailsTest : BehaviorSpec({
    val deviceTotalRewardDetails = mockk<DeviceTotalRewardsDetails>()

    beforeSpec {
        every { deviceTotalRewardDetails.isEmpty() } answers { callOriginal() }
    }

    context("Get if the deviceTotalRewardsDetails is empty or not") {
        When("the total is not null") {
            every { deviceTotalRewardDetails.total } returns 10F
            then("return false") {
                deviceTotalRewardDetails.isEmpty() shouldBe false
            }
        }
        When("the total is null") {
            every { deviceTotalRewardDetails.total } returns null
            and("the mode is not null") {
                every {
                    deviceTotalRewardDetails.mode
                } returns RewardsRepositoryImpl.Companion.RewardsSummaryMode.YEAR
                then("return false") {
                    deviceTotalRewardDetails.isEmpty() shouldBe false
                }
            }
            and("the mode is null") {
                every { deviceTotalRewardDetails.mode } returns null
                and("the totals are not empty") {
                    every { deviceTotalRewardDetails.totals } returns listOf(mockk())
                    then("return false") {
                        deviceTotalRewardDetails.isEmpty() shouldBe false
                    }
                }
                and("the totals are empty") {
                    every { deviceTotalRewardDetails.totals } returns emptyList()
                    and("the boosts are not empty") {
                        every { deviceTotalRewardDetails.boosts } returns listOf(mockk())
                        then("return false") {
                            deviceTotalRewardDetails.isEmpty() shouldBe false
                        }
                    }
                    and("the boosts are empty") {
                        every { deviceTotalRewardDetails.boosts } returns emptyList()
                        and("the status is not LOADING") {
                            every { deviceTotalRewardDetails.status } returns Status.SUCCESS
                            then("return false") {
                                deviceTotalRewardDetails.isEmpty() shouldBe false
                            }
                        }
                        and("the status is LOADING") {
                            every { deviceTotalRewardDetails.status } returns Status.LOADING
                            then("return true") {
                                deviceTotalRewardDetails.isEmpty() shouldBe true
                            }
                        }
                    }
                }
            }
        }
    }
})
