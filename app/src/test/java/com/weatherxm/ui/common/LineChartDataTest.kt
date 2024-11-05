package com.weatherxm.ui.common

import com.github.mikephil.charting.data.Entry
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class LineChartDataTest : BehaviorSpec({
    val lineChartData = mockk<LineChartData>()

    beforeSpec {
        every { lineChartData.isDataValid() } answers { callOriginal() }
        every { lineChartData.getLineDataSetsWithValues(any()) } answers { callOriginal() }
        every { lineChartData.getEmptyLineDataSets(any()) } answers { callOriginal() }
        every { lineChartData.getEntryValueForTooltip(any()) } answers { callOriginal() }
    }

    context("Get if a LineChartData is empty or not") {
        When("timestamps and entries are empty") {
            every { lineChartData.timestamps } returns mutableListOf()
            every { lineChartData.entries } returns mutableListOf()
            then("return false") {
                lineChartData.isDataValid() shouldBe false
            }
        }
        When("timestamps are not empty but entries are still empty") {
            every { lineChartData.timestamps } returns mutableListOf("timestamp")
            then("return false") {
                lineChartData.isDataValid() shouldBe false
            }
        }
        When("entries are not empty but contain only NaN values") {
            every { lineChartData.entries } returns mutableListOf(Entry(0F, Float.NaN))
            then("return false") {
                lineChartData.isDataValid() shouldBe false
            }
        }
        When("entries are not empty and contain non-NaN values") {
            every { lineChartData.entries } returns mutableListOf(Entry())
            then("return true") {
                lineChartData.isDataValid() shouldBe true
            }
        }
    }

    context("Get entry for tooltip") {
        every { lineChartData.entries } returns mutableListOf(Entry(0F, Float.NaN), Entry(1F, 1F))
        When("we need the entry for the first entry which has a NaN value") {
            then("return null") {
                lineChartData.getEntryValueForTooltip(0F) shouldBe null
            }
        }
        When("we need the entry for the second entry which has a non-NaN value") {
            then("return the value") {
                lineChartData.getEntryValueForTooltip(1F) shouldBe 1F
            }
        }
    }
})
