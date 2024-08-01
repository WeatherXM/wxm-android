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
                and("formatNumber should return EMPTY_VALUE") {
                    NumberUtils.formatNumber(null) shouldBe EMPTY_VALUE
                }
            }
            When("the number is not null") {
                then("compactNumber should return a valid number") {
                    NumberUtils.compactNumber(100) shouldBe "100"
                }
                and("formatNumber should return a valid number") {
                    NumberUtils.formatNumber(100) shouldBe "100"
                }
            }
        }
    }


    afterSpec {
        stopKoin()
    }
})
