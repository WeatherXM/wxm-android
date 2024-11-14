package com.weatherxm.analytics

import android.content.SharedPreferences
import com.weatherxm.R
import com.weatherxm.TestConfig.context
import com.weatherxm.TestConfig.sharedPref
import com.weatherxm.ui.common.WeatherUnit
import com.weatherxm.ui.common.WeatherUnitType
import com.weatherxm.util.UnitSelector
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

class AnalyticsWrapperTest : KoinTest, BehaviorSpec({
    val service1 = mockk<AnalyticsService>()
    val service2 = mockk<AnalyticsService>()
    val analyticsWrapper = AnalyticsWrapper(listOf(service1, service2), context)
    val verifier = AnalyticsWrapperTestCallVerifier(service1, service2, analyticsWrapper)
    val testUserID = "test123"
    val testDisplayMode = "dark"
    val testDevicesOwn = 6
    val testHasWallet = true

    fun AnalyticsService.mockResponses() {
        justRun { setAnalyticsEnabled(any() as Boolean) }
        justRun { onLogout() }
        justRun { setUserId(any()) }
        justRun { setUserProperties(any()) }
        justRun { trackScreen(any() as AnalyticsService.Screen, any(), any()) }
        justRun { trackEventUserAction(any(), any()) }
        justRun { trackEventViewContent(any(), any()) }
        justRun { trackEventViewContent(any(), any(), any()) }
        justRun { trackEventPrompt(any(), any(), any()) }
        justRun { trackEventSelectContent(any()) }
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

        mockkObject(UnitSelector)
        service1.mockResponses()
        service2.mockResponses()
    }

    context("Set user params and track events") {
        given("some predefined users params") {
            then("the analytics wrapper should set them properly") {
                analyticsWrapper.setUserId("")
                verifier.verifyEmptyUserIdSet("")

                analyticsWrapper.setUserId(testUserID)
                analyticsWrapper.setDevicesOwn(testDevicesOwn)
                analyticsWrapper.setHasWallet(testHasWallet)
                analyticsWrapper.setDisplayMode(testDisplayMode)
                analyticsWrapper.setDevicesSortFilterOptions(
                    listOf("DATE_ADDED", "ALL", "NO_GROUPING")
                )

                every { UnitSelector.getTemperatureUnit(context) } returns WeatherUnit(
                    WeatherUnitType.CELSIUS,
                    context.getString(R.string.temperature_celsius)
                )
                every {
                    UnitSelector.getPrecipitationUnit(context, false)
                } returns WeatherUnit(
                    WeatherUnitType.MILLIMETERS,
                    context.getString(R.string.precipitation_mm)
                )
                every { UnitSelector.getWindUnit(context) } returns WeatherUnit(
                    WeatherUnitType.MS,
                    context.getString(R.string.wind_speed_ms)
                )
                every { UnitSelector.getWindDirectionUnit(context) } returns WeatherUnit(
                    WeatherUnitType.CARDINAL,
                    context.getString(R.string.wind_direction_cardinal)
                )
                every { UnitSelector.getPressureUnit(context) } returns WeatherUnit(
                    WeatherUnitType.HPA,
                    context.getString(R.string.pressure_hpa)
                )
                with(analyticsWrapper.setUserProperties()) {
                    size shouldBe 11
                    this[0] shouldBe ("theme" to testDisplayMode)
                    this[1] shouldBe ("UNIT_TEMPERATURE" to "c")
                    this[2] shouldBe ("UNIT_WIND" to "mps")
                    this[3] shouldBe ("UNIT_WIND_DIRECTION" to "card")
                    this[4] shouldBe ("UNIT_PRECIPITATION" to "mm")
                    this[5] shouldBe ("UNIT_PRESSURE" to "hpa")
                    this[6] shouldBe ("SORT_BY" to "date_added")
                    this[7] shouldBe ("FILTER" to "all")
                    this[8] shouldBe ("GROUP_BY" to "no_grouping")
                    this[9] shouldBe ("STATIONS_OWN" to "$testDevicesOwn")
                    this[10] shouldBe ("HAS_WALLET" to "$testHasWallet")
                }
                verifier.verifyUserIdSet(testUserID)
                verifier.verifyUserPropertiesSet()
            }
        }
        given("a boolean flag indicating if analytics are enabled or not") {
            When("enabled") {
                val testArg = "testEnabled"
                then("the setter should work OK, run only one time and disable analytics") {
                    analyticsWrapper.setAnalyticsEnabled(true)
                    verifier.verifyAnalyticsEnabled(true)
                }
                then("tracking Screens should work") {
                    val screen = AnalyticsService.Screen.ANALYTICS
                    analyticsWrapper.trackScreen(screen, testArg, testArg)
                    verifier.verifyTrackScreen(screen, testArg, testArg, 1)
                }
                then("tracking User Action events should work") {
                    analyticsWrapper.trackEventUserAction(testArg, testArg)
                    verifier.verifyTrackEventUserAction(testArg, testArg, 1)
                }
                then("tracking View Content events should work") {
                    analyticsWrapper.trackEventViewContent(testArg, testArg)
                    verifier.verifyTrackEventViewContent(testArg, testArg, 1)
                }
                then("tracking failure events should work") {
                    analyticsWrapper.trackEventFailure(testArg)
                    verifier.verifyTrackEventFailure(testArg, 1)
                }
                then("tracking Prompt events should work") {
                    analyticsWrapper.trackEventPrompt(testArg, testArg, testArg)
                    verifier.verifyTrackEventPrompt(testArg, testArg, testArg, 1)
                }
                then("tracking Select Content events should work") {
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
                then("tracking screens should NOT work") {
                    val screen = AnalyticsService.Screen.ANALYTICS
                    analyticsWrapper.trackScreen(screen, testArg)
                    verifier.verifyTrackScreen(screen, testArg, null, 0)
                }
                then("tracking user action should NOT work") {
                    analyticsWrapper.trackEventUserAction(testArg)
                    verifier.verifyTrackEventUserAction(testArg, null, times = 0)
                }
                then("tracking failure events should NOT work") {
                    analyticsWrapper.trackEventFailure(testArg)
                    verifier.verifyTrackEventFailure(testArg, 0)
                }
                then("tracking view content should NOT work") {
                    analyticsWrapper.trackEventViewContent(testArg, testArg)
                    verifier.verifyTrackEventViewContent(testArg, testArg, 0)
                }
                then("tracking prompts should NOT work") {
                    analyticsWrapper.trackEventPrompt(testArg, testArg, testArg)
                    verifier.verifyTrackEventPrompt(testArg, testArg, testArg, 0)
                }
                then("tracking Select Content events should NOT work") {
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
        unmockkObject(UnitSelector)
    }
})
