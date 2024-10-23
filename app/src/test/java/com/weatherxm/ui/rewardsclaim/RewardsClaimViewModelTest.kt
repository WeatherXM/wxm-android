package com.weatherxm.ui.rewardsclaim

import android.net.Uri
import com.weatherxm.R
import com.weatherxm.TestConfig.resources
import com.weatherxm.ui.common.Contracts.ARG_TOKEN_CLAIMED_AMOUNT
import com.weatherxm.ui.common.UIWalletRewards
import com.weatherxm.util.DisplayModeHelper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class RewardsClaimViewModelTest : BehaviorSpec({
    val displayModeHelper = mockk<DisplayModeHelper>()
    val viewModel = RewardsClaimViewModel(displayModeHelper, resources)
    val testWalletRewards = UIWalletRewards(10.0, 20.0, 30.0, "0x00")

    fun mockDisplayIsSystem(expected: Boolean) {
        every { displayModeHelper.isSystem() } returns expected
    }

    val redirectUrl = "weatherxm://token-claim"
    val genericUrl = "https://weatherxm.com"
    val uri = mockk<Uri>().apply {
        every { getQueryParameter(ARG_TOKEN_CLAIMED_AMOUNT) } returns null
    }

    beforeSpec {
        every { resources.getString(R.string.weatherxm_claim_redirect_url) } returns redirectUrl
        every { displayModeHelper.getDisplayMode() } returns "dark"
    }

    context("Get Query Params") {
        given("Some data in the form of UIWalletRewards") {
            When("The Display Mode is set at system") {
                mockDisplayIsSystem(true)
                then("return the query params as String") {
                    viewModel.getQueryParams(testWalletRewards) shouldBe "?amount=30.0" +
                        "&wallet=0x00" +
                        "&redirect_url=weatherxm://token-claim" +
                        "&embed=true"
                }
            }
            When("The Display Mode is set as non-system (either dark or light)") {
                mockDisplayIsSystem(false)
                then("return the query params as String including the theme") {
                    viewModel.getQueryParams(testWalletRewards) shouldBe "?amount=30.0" +
                        "&wallet=0x00" +
                        "&redirect_url=weatherxm://token-claim" +
                        "&embed=true" +
                        "&theme=dark"
                }
            }
        }
    }

    context("Get if a URL is a redirect URL") {
        given("a URL") {
            When("it is null") {
                then("return false") {
                    viewModel.isRedirectUrl(null) shouldBe false
                }
            }
            When("it is a redirect URL") {
                then("return true") {
                    viewModel.isRedirectUrl(redirectUrl) shouldBe true
                }
            }
            When("it is not a redirect URL") {
                then("return false") {
                    viewModel.isRedirectUrl(genericUrl) shouldBe false
                }
            }
        }
    }

    context("Get amount from a redirect URI") {
        given("some data in the URI") {
            When("it is null") {
                then("return 0.0") {
                    viewModel.getAmountFromRedirectUrl(uri) shouldBe 0.0
                }
            }
            When("it is not null") {
                every { uri.getQueryParameter(ARG_TOKEN_CLAIMED_AMOUNT) } returns "10.0"
                then("return the amount") {
                    viewModel.getAmountFromRedirectUrl(uri) shouldBe 10.0
                }
            }
        }
    }
})
