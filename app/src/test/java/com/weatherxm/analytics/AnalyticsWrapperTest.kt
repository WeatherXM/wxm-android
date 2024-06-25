package com.weatherxm.analytics

import android.content.Context
import android.content.SharedPreferences
import com.weatherxm.R
import com.weatherxm.data.services.CacheService
import com.weatherxm.ui.common.empty
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.dsl.module

class AnalyticsWrapperTest : ShouldSpec() {
    private val context = mockk<Context>()
    private val analyticsWrapper = AnalyticsWrapper(mutableListOf(), context)

    init {
        should("Enable/Disable Analytics")
        {
            analyticsWrapper.setAnalyticsEnabled(false)
            analyticsWrapper.getAnalyticsEnabled() shouldBe false
            analyticsWrapper.setAnalyticsEnabled(true)
            analyticsWrapper.getAnalyticsEnabled() shouldBe true
        }

        should("Set the user ID")
        {
            analyticsWrapper.setUserId("testId")
            analyticsWrapper.getUserId() shouldBe "testId"
        }

        should("Set the Display Mode")
        {
            analyticsWrapper.setDisplayMode("dark")
            analyticsWrapper.getDisplayMode() shouldBe "dark"
        }

        should("Set the Devices Sort, Filter & Group Options")
        {
            analyticsWrapper.setDevicesSortFilterOptions(listOf("DATE_ADDED", "ALL", "NO_GROUPING"))
            val options = analyticsWrapper.getDevicesSortFilterOptions()
            options[0] shouldBe "DATE_ADDED"
            options[1] shouldBe "ALL"
            options[2] shouldBe "NO_GROUPING"
        }

        should("Set the User Properties")
        {
            val sharedPref = mockk<SharedPreferences>()
            startKoin {
                modules(
                    module {
                        single<SharedPreferences> {
                            sharedPref
                        }
                    }
                )
            }

            every { context.getString(CacheService.KEY_TEMPERATURE) } returns "temperature_unit"
            every { context.getString(R.string.temperature_celsius) } returns "°C"
            every { context.getString(CacheService.KEY_WIND) } returns "wind_speed_unit"
            every { context.getString(R.string.wind_speed_ms) } returns "bf"
            every { context.getString(CacheService.KEY_WIND_DIR) } returns "key_wind_direction_preference"
            every { context.getString(R.string.wind_direction_cardinal) } returns "Cardinal"
            every { context.getString(CacheService.KEY_PRECIP) } returns "precipitation_unit"
            every { context.getString(R.string.precipitation_mm) } returns "mm"
            every { context.getString(CacheService.KEY_PRESSURE) } returns "key_pressure_preference"
            every { context.getString(R.string.pressure_hpa) } returns "hPa"

            every { sharedPref.getString("temperature_unit", String.empty()) } returns "°C"
            every { sharedPref.getString("wind_speed_unit", String.empty()) } returns "bf"
            every {
                sharedPref.getString(
                    "key_wind_direction_preference",
                    String.empty()
                )
            } returns "Cardinal"
            every { sharedPref.getString("precipitation_unit", String.empty()) } returns "mm"
            every { sharedPref.getString("key_pressure_preference", String.empty()) } returns "hPa"

            analyticsWrapper.setDisplayMode("dark")
            analyticsWrapper.setDevicesSortFilterOptions(listOf("DATE_ADDED", "ALL", "NO_GROUPING"))
            with(analyticsWrapper.setUserProperties()) {
                size shouldBe 9
                this[0].first shouldBe "theme"
                this[0].second shouldBe "dark"
                this[1].first shouldBe "UNIT_TEMPERATURE"
                this[1].second shouldBe "c"
                this[2].first shouldBe "UNIT_WIND"
                this[2].second shouldBe "mps"
                this[3].first shouldBe "UNIT_WIND_DIRECTION"
                this[3].second shouldBe "card"
                this[4].first shouldBe "UNIT_PRECIPITATION"
                this[4].second shouldBe "mm"
                this[5].first shouldBe "UNIT_PRESSURE"
                this[5].second shouldBe "hpa"
                this[6].first shouldBe "SORT_BY"
                this[6].second shouldBe "date_added"
                this[7].first shouldBe "FILTER"
                this[7].second shouldBe "all"
                this[8].first shouldBe "GROUP_BY"
                this[8].second shouldBe "no_grouping"
            }

        }
    }
}
