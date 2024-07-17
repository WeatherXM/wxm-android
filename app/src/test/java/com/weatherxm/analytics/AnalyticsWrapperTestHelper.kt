package com.weatherxm.analytics

import android.content.Context
import android.content.SharedPreferences
import com.weatherxm.R
import com.weatherxm.data.services.CacheService
import com.weatherxm.ui.common.empty
import com.weatherxm.util.Weather
import io.mockk.*
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module

class AnalyticsWrapperTestHelper {
    val context: Context = mockk()
    val sharedPref: SharedPreferences = mockk()
    val service1: AnalyticsService = mockk()
    val service2: AnalyticsService = mockk()
    val analyticsWrapper: AnalyticsWrapper = AnalyticsWrapper(listOf(service1, service2), context)


    init {
        startKoinModules()

    }

    internal fun startKoinModules() {
        startKoin {
            modules(
                module {
                    single<SharedPreferences> { sharedPref }
                }
            )
        }
    }

    fun stopKoinModules() {
        stopKoin()
    }

    fun mockAnalyticsServiceResponses() {
        service1.mockResponses()
        service2.mockResponses()
    }

    private fun AnalyticsService.mockResponses() {
        every { setAnalyticsEnabled(any()) } just Runs
        every { setUserProperties(any(), any()) } just Runs
        every { trackScreen(any(), any(), any()) } just Runs
        every { trackEventUserAction(any(), any()) } just Runs
        every { trackEventViewContent(any(), any()) } just Runs
        every { trackEventPrompt(any(), any(), any()) } just Runs
        every { trackEventSelectContent(any()) } just Runs
    }

    fun setupContextStrings() {
        every { context.getString(CacheService.KEY_TEMPERATURE) } returns "temperature_unit"
        every { context.getString(R.string.temperature_celsius) } returns "°C"
        every { context.getString(CacheService.KEY_WIND) } returns "wind_speed_unit"
        every { context.getString(R.string.wind_speed_ms) } returns "m/s"
        every { context.getString(CacheService.KEY_WIND_DIR) } returns "key_wind_direction_preference"
        every { context.getString(R.string.wind_direction_cardinal) } returns "Cardinal"
        every { context.getString(CacheService.KEY_PRECIP) } returns "precipitation_unit"
        every { context.getString(R.string.precipitation_mm) } returns "mm"
        every { context.getString(CacheService.KEY_PRESSURE) } returns "key_pressure_preference"
        every { context.getString(R.string.pressure_hpa) } returns "hPa"

        every { sharedPref.getString("temperature_unit", String.empty()) } returns "°C"
        every { sharedPref.getString("wind_speed_unit", String.empty()) } returns "m/s"
        every {
            sharedPref.getString(
                "key_wind_direction_preference",
                String.empty()
            )
        } returns "Cardinal"
        every { sharedPref.getString("precipitation_unit", String.empty()) } returns "mm"
        every { sharedPref.getString("key_pressure_preference", String.empty()) } returns "hPa"
    }

    fun verifyUserPropertiesSet() {
        verify(exactly = 5) { Weather.getPreferredUnit(any(), any()) }
        verify { Weather.getPreferredUnit("temperature_unit", "°C") }
        verify { Weather.getPreferredUnit("wind_speed_unit", "m/s") }
        verify { Weather.getPreferredUnit("key_wind_direction_preference", "Cardinal") }
        verify { Weather.getPreferredUnit("precipitation_unit", "mm") }
        verify { Weather.getPreferredUnit("key_pressure_preference", "hPa") }
    }

    fun verifyAnalyticsEnabled(expected: Boolean) {
        verify { service1.setAnalyticsEnabled(expected) }
        verify { service2.setAnalyticsEnabled(expected) }
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
