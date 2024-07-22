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
    val verifier = AnalyticsWrapperTestCallVerifier(service1, service2, analyticsWrapper)
    val testUserID = "test123"
    val testDisplayMode = "dark"

    fun AnalyticsService.mockResponses() {
        every { setAnalyticsEnabled(any() as Boolean) } just Runs
        every { onLogout() } just Runs
        every { setUserId(any()) } just Runs
        every { setUserProperties(any()) } just Runs
        every { trackScreen(any() as AnalyticsService.Screen, any(), any()) } just Runs
        every { trackEventUserAction(any(), any()) } just Runs
        every { trackEventViewContent(any(), any()) } just Runs
        every { trackEventPrompt(any(), any(), any()) } just Runs
        every { trackEventSelectContent(any()) } just Runs
    }

    beforeSpec {
        startKoin {
            modules(
                module {
                    single<SharedPreferences> {
                        sharedPref
                    }
                }
            )
        }

        service1.mockResponses()
        service2.mockResponses()

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

        mockkObject(Weather)
    }

    context("Set user params and track events") {
        given("some predefined users params") {
            analyticsWrapper.setUserId(testUserID)
            analyticsWrapper.setDisplayMode(testDisplayMode)
            analyticsWrapper.setDevicesSortFilterOptions(listOf("DATE_ADDED", "ALL", "NO_GROUPING"))
            with(analyticsWrapper.setUserProperties()) {
                size shouldBe 9
                this[0] shouldBe ("theme" to testDisplayMode)
                this[1] shouldBe ("UNIT_TEMPERATURE" to "c")
                this[2] shouldBe ("UNIT_WIND" to "mps")
                this[3] shouldBe ("UNIT_WIND_DIRECTION" to "card")
                this[4] shouldBe ("UNIT_PRECIPITATION" to "mm")
                this[5] shouldBe ("UNIT_PRESSURE" to "hpa")
                this[6] shouldBe ("SORT_BY" to "date_added")
                this[7] shouldBe ("FILTER" to "all")
                this[8] shouldBe ("GROUP_BY" to "no_grouping")
            }
            verifier.verifyUserIdSet(testUserID)
            verifier.verifyUserPropertiesSet()
        }
        given("a boolean flag indicating if analytics are enabled or not") {
            When("enabled") {
                val testArg = "testEnabled"
                then("the setter should work OK, run only one time and disable analytics") {
                    analyticsWrapper.setAnalyticsEnabled(true)
                    verifier.verifyAnalyticsEnabled(true)
                }
                and("tracking Screens should work") {
                    val screen = AnalyticsService.Screen.ANALYTICS
                    analyticsWrapper.trackScreen(screen, testArg, testArg)
                    verifier.verifyTrackScreen(screen, testArg, testArg, 1)
                }
                and("tracking User Action events should work") {
                    analyticsWrapper.trackEventUserAction(testArg, testArg)
                    verifier.verifyTrackEventUserAction(testArg, testArg, 1)
                }
                and("tracking View Content events should work") {
                    analyticsWrapper.trackEventViewContent(testArg, testArg)
                    verifier.verifyTrackEventViewContent(testArg, testArg, 1)
                }
                and("tracking Prompt events should work") {
                    analyticsWrapper.trackEventPrompt(testArg, testArg, testArg)
                    verifier.verifyTrackEventPrompt(testArg, testArg, testArg, 1)
                }
                and("tracking Select Content events should work") {
                    analyticsWrapper.trackEventSelectContent(testArg)
                    verifier.verifyTrackEventSelectContent(testArg, 1)
                }
            }
            When("disabled") {
                val testArg = "testDisabled"
                then("the setter should work OK, run only one time and disable analytics") {
                    analyticsWrapper.setAnalyticsEnabled(false)
                    verifier.verifyAnalyticsEnabled(false)
                }
                and("tracking screens should NOT work") {
                    val screen = AnalyticsService.Screen.ANALYTICS
                    analyticsWrapper.trackScreen(screen, testArg, testArg)
                    verifier.verifyTrackScreen(screen, testArg, testArg, 0)
                }
                and("tracking user action should NOT work") {
                    analyticsWrapper.trackEventUserAction(testArg, testArg)
                    verifier.verifyTrackEventUserAction(testArg, testArg, 0)
                }
                and("tracking view content should NOT work") {
                    analyticsWrapper.trackEventViewContent(testArg, testArg)
                    verifier.verifyTrackEventViewContent(testArg, testArg, 0)
                }
                and("tracking prompts should NOT work") {
                    analyticsWrapper.trackEventPrompt(testArg, testArg, testArg)
                    verifier.verifyTrackEventPrompt(testArg, testArg, testArg, 0)
                }
                and("tracking Select Content events should NOT work") {
                    analyticsWrapper.trackEventSelectContent(testArg)
                    verifier.verifyTrackEventSelectContent(testArg, 0)
                }
            }
        }
        given("A logout event") {
            analyticsWrapper.onLogout()
            verifier.verifyOnLogout()
        }
    }

    afterSpec {
        stopKoin()
    }
})
