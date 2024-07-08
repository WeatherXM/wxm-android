package com.weatherxm.analytics

import android.content.Context
import android.content.SharedPreferences
import com.weatherxm.R
import com.weatherxm.data.services.CacheService
import com.weatherxm.ui.common.empty
import com.weatherxm.util.Weather
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

class AnalyticsWrapperTest : KoinTest, BehaviorSpec({
    val context = mockk<Context>()
    val sharedPref = mockk<SharedPreferences>()
    val service1 = mockk<AnalyticsService>()
    val service2 = mockk<AnalyticsService>()
    val analyticsWrapper = AnalyticsWrapper(listOf(service1, service2), context)

    startKoin {
        modules(
            module {
                single<SharedPreferences> {
                    sharedPref
                }
            }
        )
    }

    fun AnalyticsService.mockResponses() {
        every { setAnalyticsEnabled(any() as Boolean) } just Runs
        every { setUserProperties(any() as String, any()) } just Runs
        every { trackScreen(any() as AnalyticsService.Screen, any(), any()) } just Runs
        every { trackEventUserAction(any(), any()) } just Runs
        every { trackEventViewContent(any(), any()) } just Runs
        every { trackEventPrompt(any(), any(), any()) } just Runs
        every { trackEventSelectContent(any()) } just Runs
    }

    beforeSpec {
        service1.mockResponses()
        service2.mockResponses()
    }

    context("Set user params and track events") {
        given("some predefined users params") {
            analyticsWrapper.setUserId("testId")

            every { context.getString(CacheService.KEY_TEMPERATURE) } returns "temperature_unit"
            every { context.getString(R.string.temperature_celsius) } returns "°C"
            every { context.getString(CacheService.KEY_WIND) } returns "wind_speed_unit"
            every { context.getString(R.string.wind_speed_ms) } returns "m/s"
            every {
                context.getString(CacheService.KEY_WIND_DIR)
            } returns "key_wind_direction_preference"
            every { context.getString(R.string.wind_direction_cardinal) } returns "Cardinal"
            every { context.getString(CacheService.KEY_PRECIP) } returns "precipitation_unit"
            every { context.getString(R.string.precipitation_mm) } returns "mm"
            every { context.getString(CacheService.KEY_PRESSURE) } returns "key_pressure_preference"
            every { context.getString(R.string.pressure_hpa) } returns "hPa"

            every { sharedPref.getString("temperature_unit", String.empty()) } returns "°C"
            every { sharedPref.getString("wind_speed_unit", String.empty()) } returns "m/s"
            every {
                sharedPref.getString("key_wind_direction_preference", String.empty())
            } returns "Cardinal"
            every { sharedPref.getString("precipitation_unit", String.empty()) } returns "mm"
            every { sharedPref.getString("key_pressure_preference", String.empty()) } returns "hPa"

            analyticsWrapper.setDisplayMode("dark")
            analyticsWrapper.setDevicesSortFilterOptions(listOf("DATE_ADDED", "ALL", "NO_GROUPING"))
            analyticsWrapper.getUserId() shouldBe "testId"
            mockkObject(Weather)
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
            verify(exactly = 5) { Weather.getPreferredUnit(any(), any()) }
            verify(exactly = 1) { Weather.getPreferredUnit("temperature_unit", "°C") }
            verify(exactly = 1) { Weather.getPreferredUnit("wind_speed_unit", "m/s") }
            verify(exactly = 1) {
                Weather.getPreferredUnit("key_wind_direction_preference", "Cardinal")
            }
            verify(exactly = 1) { Weather.getPreferredUnit("precipitation_unit", "mm") }
            verify(exactly = 1) { Weather.getPreferredUnit("key_pressure_preference", "hPa") }
        }
        given("a boolean flag indicating if analytics are enabled or not") {
            When("enabled") {
                val testArg = "testEnabled"
                then("the setter should work OK, run only one time and disable analytics") {
                    analyticsWrapper.setAnalyticsEnabled(true)
                    analyticsWrapper.getAnalyticsEnabled() shouldBe true
                    verify(exactly = 1) { service1.setAnalyticsEnabled(true) }
                    verify(exactly = 0) { service1.setAnalyticsEnabled(false) }
                    verify(exactly = 1) { service2.setAnalyticsEnabled(true) }
                    verify(exactly = 0) { service2.setAnalyticsEnabled(false) }
                }
                and("tracking Screens should work") {
                    val screen = AnalyticsService.Screen.ANALYTICS
                    analyticsWrapper.trackScreen(screen, testArg, testArg)
                    verify(exactly = 1) { service1.trackScreen(screen, testArg, testArg) }
                    verify(exactly = 1) { service2.trackScreen(screen, testArg, testArg) }
                }
                and("tracking User Action events should work") {
                    analyticsWrapper.trackEventUserAction(testArg, testArg)
                    verify(exactly = 1) { service1.trackEventUserAction(testArg, testArg) }
                    verify(exactly = 1) { service2.trackEventUserAction(testArg, testArg) }
                }
                and("tracking View Content events should work") {
                    analyticsWrapper.trackEventViewContent(testArg, testArg)
                    verify(exactly = 1) { service1.trackEventViewContent(testArg, testArg) }
                    verify(exactly = 1) { service2.trackEventViewContent(testArg, testArg) }
                }
                and("tracking Prompt events should work") {
                    analyticsWrapper.trackEventPrompt(testArg, testArg, testArg)
                    verify(exactly = 1) { service1.trackEventPrompt(testArg, testArg, testArg) }
                    verify(exactly = 1) { service2.trackEventPrompt(testArg, testArg, testArg) }
                }
                and("tracking Select Content events should work") {
                    analyticsWrapper.trackEventSelectContent(testArg)
                    verify(exactly = 1) { service1.trackEventSelectContent(testArg) }
                    verify(exactly = 1) { service2.trackEventSelectContent(testArg) }
                }
            }
            When("disabled") {
                val testArg = "testDisabled"
                then("the setter should work OK, run only one time and disable analytics") {
                    analyticsWrapper.setAnalyticsEnabled(false)
                    verify(exactly = 1) { service1.setAnalyticsEnabled(false) }
                    verify(exactly = 1) { service2.setAnalyticsEnabled(false) }
                    analyticsWrapper.getAnalyticsEnabled() shouldBe false
                }
                and("tracking screens should NOT work") {
                    val screen = AnalyticsService.Screen.ANALYTICS
                    analyticsWrapper.trackScreen(screen, testArg, testArg)
                    verify(exactly = 0) { service1.trackScreen(screen, testArg, testArg) }
                    verify(exactly = 0) { service2.trackScreen(screen, testArg, testArg) }
                }
                and("tracking user action should NOT work") {
                    analyticsWrapper.trackEventUserAction(testArg, testArg)
                    verify(exactly = 0) { service1.trackEventUserAction(testArg, testArg) }
                    verify(exactly = 0) { service2.trackEventUserAction(testArg, testArg) }
                }
                and("tracking view content should NOT work") {
                    analyticsWrapper.trackEventViewContent(testArg, testArg)
                    verify(exactly = 0) { service1.trackEventViewContent(testArg, testArg) }
                    verify(exactly = 0) { service2.trackEventViewContent(testArg, testArg) }
                }
                and("tracking prompts should NOT work") {
                    analyticsWrapper.trackEventPrompt(testArg, testArg, testArg)
                    verify(exactly = 0) { service1.trackEventPrompt(testArg, testArg, testArg) }
                    verify(exactly = 0) { service2.trackEventPrompt(testArg, testArg, testArg) }
                }
                and("tracking Select Content events should NOT work") {
                    analyticsWrapper.trackEventSelectContent(testArg)
                    verify(exactly = 0) { service1.trackEventSelectContent(testArg) }
                    verify(exactly = 0) { service2.trackEventSelectContent(testArg) }
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
