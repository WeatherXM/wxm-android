package com.weatherxm.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ChartsTest : BehaviorSpec({
    val times = mutableListOf("12:00", "13:00", "14:00", "15:00")
    val xAxisFormatter = CustomXAxisFormatter(times)
    val yAxisFormatter = CustomYAxisFormatter()
    val yAxisFormatterWithDecimals = CustomYAxisFormatter(1)

    context("Use custom X axis formatter") {
        given("A list of times") {
            then("it should return the correct time") {
                xAxisFormatter.getAxisLabel(0F, null) shouldBe "12:00"
                xAxisFormatter.getAxisLabel(1F, null) shouldBe "13:00"
                xAxisFormatter.getAxisLabel(5F, null) shouldBe "?"
            }
        }
    }

    context("Use custom Y axis formatter") {
        given("A value") {
            When("it is less than 10000") {
                and("The formatter does not support decimals") {
                    then("it should return the correct value") {
                        yAxisFormatter.getAxisLabel(1000.65F, null) shouldBe "1001"
                    }
                }
                and("The formatter supports decimals") {
                    then("it should return the correct value") {
                        yAxisFormatterWithDecimals.getAxisLabel(1000.65F, null) shouldBe "1000.7"
                    }
                }
            }
            When("it is greater than 10000") {
                then("it should return the correct value") {
                    yAxisFormatter.getAxisLabel(100000F, null) shouldBe "100K"
                }
            }
        }
    }
})
