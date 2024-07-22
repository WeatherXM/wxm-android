package com.weatherxm.util

import com.weatherxm.R
import com.weatherxm.ui.common.AnnotationGroupCode
import com.weatherxm.util.Rewards.formatTokens
import com.weatherxm.util.Rewards.getRewardIcon
import com.weatherxm.util.Rewards.getRewardScoreColor
import com.weatherxm.util.Rewards.isPoL
import com.weatherxm.util.Rewards.weiToETH
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class RewardsTest : BehaviorSpec({
    context("Get reward score color and icon") {
        given("a reward score") {
            When("it is null or non-supported") {
                then("the color should be `unknown`") {
                    getRewardScoreColor(null) shouldBe R.color.reward_score_unknown
                    getRewardScoreColor(-5) shouldBe R.color.reward_score_unknown
                }
                and("the icon should be a `warning`") {
                    getRewardIcon(null) shouldBe R.drawable.ic_warning_hex_filled
                    getRewardIcon(-5) shouldBe R.drawable.ic_warning_hex_filled
                }
            }
            When("it is so low that an error should be visible") {
                then("the color should be `error`") {
                    getRewardScoreColor(0) shouldBe R.color.error
                    getRewardScoreColor(5) shouldBe R.color.error
                }
                and("the icon should be an `error`") {
                    getRewardIcon(0) shouldBe R.drawable.ic_error_hex_filled
                    getRewardIcon(5) shouldBe R.drawable.ic_error_hex_filled
                }
            }
            When("it is between error and good so that a warning should be visible") {
                then("the color should be `warning`") {
                    getRewardScoreColor(15) shouldBe R.color.warning
                    getRewardScoreColor(35) shouldBe R.color.warning
                    getRewardScoreColor(55) shouldBe R.color.warning
                    getRewardScoreColor(75) shouldBe R.color.warning
                }
                and("the icon should be a `warning`") {
                    getRewardIcon(15) shouldBe R.drawable.ic_warning_hex_filled
                    getRewardIcon(35) shouldBe R.drawable.ic_warning_hex_filled
                    getRewardIcon(55) shouldBe R.drawable.ic_warning_hex_filled
                    getRewardIcon(75) shouldBe R.drawable.ic_warning_hex_filled
                }
            }
            When("it is good so that everything is OK") {
                then("the color should be `green`") {
                    getRewardScoreColor(95) shouldBe R.color.green
                    getRewardScoreColor(100) shouldBe R.color.green
                }
                and("the icon should be a `checkmark`") {
                    getRewardIcon(95) shouldBe R.drawable.ic_checkmark_hex_filled
                    getRewardIcon(100) shouldBe R.drawable.ic_checkmark_hex_filled
                }
            }
        }
    }
    context("Formatting tokens as a text with min 2 and max 3 decimals") {
        given("a token amount") {
            When("it is zero") {
                then("the formatter should return 0.00") {
                    formatTokens(0F) shouldBe "0.00"
                }
            }
            When("it is an integer") {
                then("the formatter should return the integer with 2 decimals e.g. 10.00") {
                    formatTokens(10F) shouldBe "10.00"
                }
            }
            When("it has up to 2 decimals") {
                then("the formatter should return the amount with 2 decimals") {
                    formatTokens(10.1F) shouldBe "10.10"
                    formatTokens(10.01F) shouldBe "10.01"
                }
            }
            When("it has >=3 decimals") {
                then("the formatter should return the amount with 3 or 4 decimals") {
                    formatTokens(10.001F) shouldBe "10.001"
                    formatTokens(10.0001F) shouldBe "10.0001"
                    formatTokens(10.0006F) shouldBe "10.0006"
                    formatTokens(10.00006F) shouldBe "10.0001"
                }
            }
        }
    }
    context("Convert WEI -> ETH correctly and show the correct amount") {
        given("a WEI amount") {
            When("it is zero") {
                then("the convertor should return zero") {
                    weiToETH(BigDecimal.ZERO) shouldBe BigDecimal.ZERO
                }
            }
            When("it is bigger than zero") {
                then("the validator should return make the conversion and format the tokens") {
                    formatTokens(weiToETH(9.9999999E14F.toBigDecimal())) shouldBe "0.001"
                }
            }
        }
    }
    context("Get if an AnnotationGroupCode is Proof-of-Location related") {
        given("an AnnotationGroupCode") {
            When("it is PoL-related") {
                then("the checker function should return true") {
                    AnnotationGroupCode.LOCATION_NOT_VERIFIED.isPoL() shouldBe true
                    AnnotationGroupCode.NO_LOCATION_DATA.isPoL() shouldBe true
                    AnnotationGroupCode.USER_RELOCATION_PENALTY.isPoL() shouldBe true
                }
            }
            When("it is non-PoL related") {
                then("the checker function should return false") {
                    AnnotationGroupCode.NO_WALLET.isPoL() shouldBe false
                    AnnotationGroupCode.CELL_CAPACITY_REACHED.isPoL() shouldBe false
                    AnnotationGroupCode.UNKNOWN.isPoL() shouldBe false
                }
            }
        }
    }
})
