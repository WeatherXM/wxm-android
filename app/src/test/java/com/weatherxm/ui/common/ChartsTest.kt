package com.weatherxm.ui.common

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.scopes.BehaviorSpecWhenContainerScope
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class ChartsTest : BehaviorSpec({
    val charts = mockk<Charts>()

    beforeSpec {
        every { charts.isEmpty() } answers { callOriginal() }
        every { charts.temperature } returns mockk()
        every { charts.feelsLike } returns mockk()
        every { charts.precipitation } returns mockk()
        every { charts.precipitationAccumulated } returns mockk()
        every { charts.precipProbability } returns mockk()
        every { charts.windSpeed } returns mockk()
        every { charts.windGust } returns mockk()
        every { charts.windDirection } returns mockk()
        every { charts.humidity } returns mockk()
        every { charts.pressure } returns mockk()
        every { charts.uv } returns mockk()
        every { charts.solarRadiation } returns mockk()
    }

    suspend fun BehaviorSpecWhenContainerScope.verifyNotEmptyCharts(data: LineChartData) {
        every { data.isDataValid() } returns true
        then("return false") {
            charts.isEmpty() shouldBe false
        }
        every { data.isDataValid() } returns false
    }

    context("Get if charts are empty or not") {
        When("temperature data is valid") {
            verifyNotEmptyCharts(charts.temperature)
        }
        When("feels like data is valid") {
            verifyNotEmptyCharts(charts.feelsLike)
        }
        When("precipitation data is valid") {
            verifyNotEmptyCharts(charts.precipitation)
        }
        When("precipitation accumulated data is valid") {
            verifyNotEmptyCharts(charts.precipitationAccumulated)
        }
        When("wind speed data is valid") {
            verifyNotEmptyCharts(charts.windSpeed)
        }
        When("wind gust data is valid") {
            verifyNotEmptyCharts(charts.windGust)
        }
        When("precipitation probability data is valid") {
            verifyNotEmptyCharts(charts.precipProbability)
        }
        When("wind direction data is valid") {
            verifyNotEmptyCharts(charts.windDirection)
        }
        When("humidity data is valid") {
            verifyNotEmptyCharts(charts.humidity)
        }
        When("pressure data is valid") {
            verifyNotEmptyCharts(charts.pressure)
        }
        When("uv data is valid") {
            verifyNotEmptyCharts(charts.uv)
        }
        When("solar radiation data is valid") {
            verifyNotEmptyCharts(charts.solarRadiation)
        }
    }
})
