package com.weatherxm.util

import android.icu.text.CompactDecimalFormat
import android.icu.text.NumberFormat
import com.weatherxm.ui.common.empty
import com.weatherxm.util.NumberUtils.formatTokens
import com.weatherxm.util.NumberUtils.weiToETH
import com.weatherxm.util.Weather.EMPTY_VALUE
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.math.BigDecimal

class NumberUtilsTest : BehaviorSpec({
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
        every { NumberUtils.compactNumber(any()) } returns "100"
        every { NumberUtils.formatNumber(any()) } returns "100"
    }

    context("Formatting of numbers") {
        given("A nullable number") {
            When("the number is null") {
                then("compactNumber should return EMPTY_VALUE") {
                    NumberUtils.compactNumber(null) shouldBe EMPTY_VALUE
                }
                then("formatNumber should return EMPTY_VALUE") {
                    NumberUtils.formatNumber(null) shouldBe EMPTY_VALUE
                }
            }
            When("the number is not null") {
                then("compactNumber should return a valid number") {
                    NumberUtils.compactNumber(100) shouldBe "100"
                }
                then("formatNumber should return a valid number") {
                    NumberUtils.formatNumber(100) shouldBe "100"
                }
                then("roundToDecimals should return a valid number") {
                    NumberUtils.roundToDecimals(100.123456) shouldBe 100.1F
                    NumberUtils.roundToDecimals(100.15) shouldBe 100.2F
                    NumberUtils.roundToDecimals(100.02) shouldBe 100.0F
                }
                then("roundToInt should return a valid number") {
                    NumberUtils.roundToInt(1.0) shouldBe 1
                    NumberUtils.roundToInt(1.49) shouldBe 1
                    NumberUtils.roundToInt(1.5) shouldBe 2
                    NumberUtils.roundToInt(1.51) shouldBe 2
                }
            }
        }
    }

    context("Formatting tokens as a text with min 2 and max 3 decimals") {
        given("a token amount") {
            When("it is null") {
                then("the formatter should return an empty string") {
                    formatTokens(null) shouldBe String.empty()
                }
            }
            When("it is zero") {
                then("the formatter should return 0.00") {
                    formatTokens(0F) shouldBe "0.00"
                }
            }
            When("it is an integer") {
                and("it is == 1000") {
                    then("return the integer with thousands separator and 2 decimals") {
                        formatTokens(1000F) shouldBe "1,000.00"
                    }
                }
                and("it is < 1000") {
                    then("the formatter should return the integer with 2 decimals e.g. 10.00") {
                        formatTokens(10F) shouldBe "10.00"
                    }
                }
            }
            When("it has up to 2 decimals") {
                then("the formatter should return the amount with 2 decimals") {
                    formatTokens(10.1F) shouldBe "10.10"
                    formatTokens(10.01F) shouldBe "10.01"
                }
            }
            When("it has >=3 decimals") {
                then("the formatter should return the amount with 2 decimals") {
                    formatTokens(10.001F) shouldBe "10.00"
                    formatTokens(10.005F) shouldBe "10.01"
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
                    formatTokens(weiToETH(BigDecimal.valueOf(10000000000000000))) shouldBe "0.01"
                }
            }
        }
    }


    afterSpec {
        stopKoin()
    }
})
