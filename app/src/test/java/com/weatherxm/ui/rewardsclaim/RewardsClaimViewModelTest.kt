package com.weatherxm.ui.rewardsclaim

import com.weatherxm.R
import com.weatherxm.ui.common.UIWalletRewards
import com.weatherxm.util.DisplayModeHelper
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class RewardsClaimViewModelTest : BehaviorSpec({
    val displayModeHelper = mockk<DisplayModeHelper>()
    val resources = mockk<Resources>()
    val viewModel = RewardsClaimViewModel(displayModeHelper, resources)

    beforeSpec {
        every {
            resources.getString(R.string.weatherxm_claim_redirect_url)
        } returns "weatherxm://token-claim"
        every { displayModeHelper.getDisplayMode() } returns "dark"
    }

    context("Get Query Params") {
        given("Some data in the form of UIWalletRewards") {
            When("The Display Mode is set at system") {
                every { displayModeHelper.isSystem() } returns true
                then("return the query params as String") {
                    val queryParams =
                        viewModel.getQueryParams(UIWalletRewards(10.0, 20.0, 30.0, "0x00"))
                    queryParams shouldBe "?amount=30.0" +
                        "&wallet=0x00" +
                        "&redirect_url=weatherxm://token-claim" +
                        "&embed=true"
                }
            }
            When("The Display Mode is set as non-system (either dark or light)") {
                every { displayModeHelper.isSystem() } returns false
                then("return the query params as String") {
                    val queryParams =
                        viewModel.getQueryParams(UIWalletRewards(10.0, 20.0, 30.0, "0x00"))
                    queryParams shouldBe "?amount=30.0" +
                        "&wallet=0x00" +
                        "&redirect_url=weatherxm://token-claim" +
                        "&embed=true" +
                        "&theme=dark"
                }
            }
        }
    }
})
