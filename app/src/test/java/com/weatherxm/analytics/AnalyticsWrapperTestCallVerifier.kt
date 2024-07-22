package com.weatherxm.analytics

import com.weatherxm.util.Weather
import io.kotest.matchers.shouldBe
import io.mockk.verify

class AnalyticsWrapperTestCallVerifier(
    private val service1: AnalyticsService,
    private val service2: AnalyticsService,
    private val analyticsWrapper: AnalyticsWrapper
) {
    fun verifyUserIdSet(userId: String) {
        verify(exactly = 1) { service1.setUserId(userId) }
        verify(exactly = 1) { service2.setUserId(userId) }
    }

    fun verifyUserPropertiesSet() {
        verify(exactly = 5) { Weather.getPreferredUnit(any(), any()) }
        verify(exactly = 1) { Weather.getPreferredUnit("temperature_unit", "°C") }
        verify(exactly = 1) { Weather.getPreferredUnit("wind_speed_unit", "m/s") }
        verify(exactly = 1) {
            Weather.getPreferredUnit("key_wind_direction_preference", "Cardinal")
        }
        verify(exactly = 1) { Weather.getPreferredUnit("precipitation_unit", "mm") }
        verify(exactly = 1) { Weather.getPreferredUnit("key_pressure_preference", "hPa") }
    }

    fun verifyAnalyticsEnabled(enabled: Boolean) {
        analyticsWrapper.getAnalyticsEnabled() shouldBe enabled
        verify(exactly = 1) { service1.setAnalyticsEnabled(enabled) }
        verify(exactly = 1) { service2.setAnalyticsEnabled(enabled) }
    }

    fun verifyOnLogout() {
        verify(exactly = 1) { service1.onLogout() }
        verify(exactly = 1) { service2.onLogout() }
    }

    fun verifyTrackScreen(screen: AnalyticsService.Screen, arg1: String, arg2: String, times: Int) {
        verify(exactly = times) { service1.trackScreen(screen, arg1, arg2) }
        verify(exactly = times) { service2.trackScreen(screen, arg1, arg2) }
    }

    fun verifyTrackEventUserAction(arg1: String, arg2: String, times: Int) {
        verify(exactly = times) { service1.trackEventUserAction(arg1, arg2) }
        verify(exactly = times) { service2.trackEventUserAction(arg1, arg2) }
    }

    fun verifyTrackEventViewContent(arg1: String, arg2: String, times: Int) {
        verify(exactly = times) { service1.trackEventViewContent(arg1, arg2) }
        verify(exactly = times) { service2.trackEventViewContent(arg1, arg2) }
    }

    fun verifyTrackEventPrompt(arg1: String, arg2: String, arg3: String, times: Int) {
        verify(exactly = times) { service1.trackEventPrompt(arg1, arg2, arg3) }
        verify(exactly = times) { service2.trackEventPrompt(arg1, arg2, arg3) }
    }

    fun verifyTrackEventSelectContent(arg: String, times: Int) {
        verify(exactly = times) { service1.trackEventSelectContent(arg) }
        verify(exactly = times) { service2.trackEventSelectContent(arg) }
    }
}
