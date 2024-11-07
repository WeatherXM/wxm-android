package com.weatherxm.ui.common

import com.weatherxm.data.models.RewardSplit
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlin.random.Random
import kotlin.random.nextInt

class RewardSplitsDataTest : BehaviorSpec({
    val rewardSplitsData = mockk<RewardSplitsData>()

    beforeSpec {
        every { rewardSplitsData.hasSplitRewards() } answers { callOriginal() }
    }

    context("Get if a RewardSplitsData has split rewards") {
        When("the list size is smaller than two") {
            val splits = mutableListOf<RewardSplit>()
            repeat(Random.nextInt(0..1)) {
                splits.add(mockk())
            }
            every { rewardSplitsData.splits } returns splits
            then("return false") {
                rewardSplitsData.hasSplitRewards() shouldBe false
            }
        }
        When("the list size is equal or higher than two") {
            val splits = mutableListOf<RewardSplit>()
            repeat(Random.nextInt(2..10)) {
                splits.add(mockk())
            }
            every { rewardSplitsData.splits } returns splits
            then("return true") {
                rewardSplitsData.hasSplitRewards() shouldBe true
            }
        }
    }
})
