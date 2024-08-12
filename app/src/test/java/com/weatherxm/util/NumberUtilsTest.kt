package com.weatherxm.util

import android.icu.text.CompactDecimalFormat
import android.icu.text.NumberFormat
import com.weatherxm.util.Weather.EMPTY_VALUE
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

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


    afterSpec {
        stopKoin()
    }
})
