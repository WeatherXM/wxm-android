package com.weatherxm.ui.common

import android.graphics.Color
import android.util.Log
import com.github.mikephil.charting.data.Entry
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic

class LineChartDataTest : BehaviorSpec({
    val mockLineChartData = mockk<LineChartData>()
    val label = "label"
    val lineChartData = LineChartData(
        mutableListOf(),
        mutableListOf(
            Entry(0F, 0F),
            Entry(1F, Float.NaN),
            Entry(2F, Float.NaN),
            Entry(3F, 2F),
            Entry(4F, 3F),
            Entry(5F, Float.NaN),
            Entry(6F, 5F)
        )
    )
    val emptyLineChartData = LineChartData(mutableListOf(), mutableListOf())

    beforeSpec {
        every { mockLineChartData.isDataValid() } answers { callOriginal() }
        every { mockLineChartData.getLineDataSetsWithValues(any()) } answers { callOriginal() }
        every { mockLineChartData.getEmptyLineDataSets(any()) } answers { callOriginal() }
        every { mockLineChartData.getEntryValueForTooltip(any()) } answers { callOriginal() }

        mockkStatic(Color::class)
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Color.rgb(any() as Int, any(), any()) } returns 0
    }

    context("Get if a LineChartData is empty or not") {
        When("timestamps and entries are empty") {
            every { mockLineChartData.timestamps } returns mutableListOf()
            every { mockLineChartData.entries } returns mutableListOf()
            then("return false") {
                mockLineChartData.isDataValid() shouldBe false
            }
        }
        When("timestamps are not empty but entries are still empty") {
            every { mockLineChartData.timestamps } returns mutableListOf("timestamp")
            then("return false") {
                mockLineChartData.isDataValid() shouldBe false
            }
        }
        When("entries are not empty but contain only NaN values") {
            every { mockLineChartData.entries } returns mutableListOf(Entry(0F, Float.NaN))
            then("return false") {
                mockLineChartData.isDataValid() shouldBe false
            }
        }
        When("entries are not empty and contain non-NaN values") {
            every { mockLineChartData.entries } returns mutableListOf(Entry())
            then("return true") {
                mockLineChartData.isDataValid() shouldBe true
            }
        }
    }

    context("Get entry for tooltip") {
        every { mockLineChartData.entries } returns mutableListOf(
            Entry(0F, Float.NaN),
            Entry(1F, 1F)
        )
        When("we need the entry for the first entry which has a NaN value") {
            then("return null") {
                mockLineChartData.getEntryValueForTooltip(0F) shouldBe null
            }
        }
        When("we need the entry for the second entry which has a non-NaN value") {
            then("return the value") {
                mockLineChartData.getEntryValueForTooltip(1F) shouldBe 1F
            }
        }
    }

    context("Get a list of LineDataSet with values (continuous lines with no gaps)") {
        When("we have an empty LineChartData object") {
            then("return an empty list of LineDataSet") {
                emptyLineChartData.getLineDataSetsWithValues(label) shouldBe emptyList()
            }
        }
        When("we have a LineChartData object with values") {
            then("get the list of the LineDataSet with values") {
                lineChartData.getLineDataSetsWithValues(label).apply {
                    size shouldBe 3

                    get(0).entryCount shouldBe 1
                    get(0).getEntryForIndex(0).also {
                        it.x shouldBe 0.0
                        it.y shouldBe 0.0
                    }

                    get(1).entryCount shouldBe 2
                    get(1).getEntryForIndex(0).also {
                        it.x shouldBe 3.0
                        it.y shouldBe 2.0
                    }
                    get(1).getEntryForIndex(1).also {
                        it.x shouldBe 4.0
                        it.y shouldBe 3.0
                    }

                    get(2).entryCount shouldBe 1
                    get(2).getEntryForIndex(0).also {
                        it.x shouldBe 6.0
                        it.y shouldBe 5.0
                    }
                }
            }
        }
    }

    context("Get a list of LineDataSet of empty values (gaps)") {
        When("we have an empty LineChartData object") {
            then("return an empty list of LineDataSet") {
                emptyLineChartData.getEmptyLineDataSets(label) shouldBe emptyList()
            }
        }
        When("we have a LineChartData object with some gaps") {
            then("get the list of the LineDataSet of the gaps") {
                lineChartData.getEmptyLineDataSets(label).apply {
                    size shouldBe 2

                    get(0).entryCount shouldBe 2
                    get(0).getEntryForIndex(0).also {
                        it.x shouldBe 1.0
                        it.y shouldBe Float.NaN
                    }
                    get(0).getEntryForIndex(1).also {
                        it.x shouldBe 2.0
                        it.y shouldBe Float.NaN
                    }

                    get(1).entryCount shouldBe 1
                    get(1).getEntryForIndex(0).also {
                        it.x shouldBe 5.0
                        it.y shouldBe Float.NaN
                    }
                }
            }
        }
    }

    afterSpec {
        unmockkStatic(Color::class)
        unmockkStatic(Log::class)
    }
})
