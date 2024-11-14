package com.weatherxm

import android.content.Context
import android.content.SharedPreferences
import android.location.Geocoder
import android.text.format.DateFormat
import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.TestUtils.retrofitResponse
import com.weatherxm.data.DATE_FORMAT_MONTH_DAY
import com.weatherxm.data.models.Failure
import com.weatherxm.data.network.ErrorResponse
import com.weatherxm.data.services.CacheService.Companion.KEY_ANALYTICS
import com.weatherxm.data.services.CacheService.Companion.KEY_PRECIP
import com.weatherxm.data.services.CacheService.Companion.KEY_PRESSURE
import com.weatherxm.data.services.CacheService.Companion.KEY_TEMPERATURE
import com.weatherxm.data.services.CacheService.Companion.KEY_THEME
import com.weatherxm.data.services.CacheService.Companion.KEY_WIND
import com.weatherxm.data.services.CacheService.Companion.KEY_WIND_DIR
import com.weatherxm.ui.common.empty
import com.weatherxm.util.AndroidBuildInfo
import com.weatherxm.util.CountDownTimerHelper
import com.weatherxm.util.Resources
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.listeners.AfterProjectListener
import io.kotest.core.listeners.BeforeProjectListener
import io.kotest.core.names.DuplicateTestNameMode
import io.kotest.core.spec.AutoScan
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import java.util.Locale

object TestConfig : AbstractProjectConfig() {
    override val duplicateTestNameMode = DuplicateTestNameMode.Error
    val context = mockk<Context>()
    val resources = mockk<Resources>()
    val sharedPref = mockk<SharedPreferences>()
    val failure = mockk<Failure>()
    val successUnitResponse =
        NetworkResponse.Success<Unit, ErrorResponse>(Unit, retrofitResponse(Unit))
    val geocoder = mockk<Geocoder>()

    const val REACH_OUT_MSG = "Reach Out"
    const val DEVICE_NOT_FOUND_MSG = "Device Not Found"
    const val NO_CONNECTION_MSG = "No Connection"
    const val CONNECTION_TIMEOUT_MSG = "Connection Timeout"

    @AutoScan
    object MyProjectListener : BeforeProjectListener, AfterProjectListener {
        @Suppress("LongMethod")
        override suspend fun beforeProject() {
            mockkConstructor(CountDownTimerHelper::class)
            justRun { anyConstructed<CountDownTimerHelper>().start(any(), any()) }
            justRun { anyConstructed<CountDownTimerHelper>().stop() }
            mockkStatic(Geocoder::class)
            mockkStatic(DateFormat::class)
            mockkObject(AndroidBuildInfo)
            every {
                DateFormat.getBestDateTimePattern(
                    Locale.getDefault(),
                    DATE_FORMAT_MONTH_DAY
                )
            } returns "d/M"
            every { failure.code } returns String.empty()
            every { resources.getString(R.string.error_reach_out) } returns REACH_OUT_MSG
            every { resources.getString(R.string.error_reach_out_short) } returns REACH_OUT_MSG
            every { resources.getString(R.string.error_network_generic) } returns NO_CONNECTION_MSG
            every {
                resources.getString(R.string.error_network_timed_out)
            } returns CONNECTION_TIMEOUT_MSG
            every {
                resources.getString(R.string.error_device_not_found)
            } returns DEVICE_NOT_FOUND_MSG
            every { context.getString(R.string.uv_low) } returns "Low"
            every { context.getString(R.string.uv_moderate) } returns "Moderate"
            every { context.getString(R.string.uv_high) } returns "High"
            every { context.getString(R.string.uv_very_high) } returns "Very High"
            every { context.getString(R.string.uv_extreme) } returns "Extreme"
            every { context.getString(KEY_TEMPERATURE) } returns "temperature_unit"
            every { resources.getString(KEY_TEMPERATURE) } returns "temperature_unit"
            every { resources.getString(R.string.temperature_celsius) } returns "°C"
            every { context.getString(R.string.temperature_celsius) } returns "°C"
            every { context.getString(R.string.temperature_fahrenheit) } returns "°F"
            every { sharedPref.getString("temperature_unit", "°C") } returns "°C"
            every { context.getString(R.string.solar_radiation_unit) } returns "W/m2"
            every { resources.getString(KEY_PRESSURE) } returns "key_pressure_preference"
            every { resources.getString(R.string.pressure_hpa) } returns "hPa"
            every { context.getString(R.string.pressure_hpa) } returns "hPa"
            every { context.getString(R.string.pressure_inHg) } returns "inHg"
            every { sharedPref.getString("key_pressure_preference", "hPa") } returns "hPa"
            every { resources.getString(KEY_PRECIP) } returns "precipitation_unit"
            every { resources.getString(R.string.precipitation_mm) } returns "mm"
            every { context.getString(R.string.precipitation_mm) } returns "mm"
            every { context.getString(R.string.precipitation_mm_hour) } returns "mm/h"
            every { context.getString(R.string.precipitation_in) } returns "in"
            every { context.getString(R.string.precipitation_in_hour) } returns "in/h"
            every { sharedPref.getString("precipitation_unit", "mm") } returns "mm"
            every { resources.getString(KEY_WIND) } returns "wind_speed_unit"
            every { resources.getString(R.string.wind_speed_ms) } returns "m/s"
            every { context.getString(R.string.wind_speed_ms) } returns "m/s"
            every { context.getString(R.string.wind_speed_beaufort) } returns "bf"
            every { context.getString(R.string.wind_speed_kmh) } returns "km/h"
            every { context.getString(R.string.wind_speed_mph) } returns "mph"
            every { context.getString(R.string.wind_speed_knots) } returns "knots"
            every { sharedPref.getString("wind_speed_unit", "m/s") } returns "m/s"
            every { resources.getString(KEY_WIND_DIR) } returns "key_wind_direction_preference"
            every { resources.getString(R.string.wind_direction_cardinal) } returns "Cardinal"
            every { context.getString(R.string.wind_direction_cardinal) } returns "Cardinal"
            every { context.getString(R.string.wind_direction_degrees) } returns "Degrees"
            every {
                sharedPref.getString("key_wind_direction_preference", "Cardinal")
            } returns "Cardinal"
            every { context.getString(R.string.wxm_amount, any() as String) } returns "10 \$WXM"
            every {
                context.getString(R.string.rewardable_station_hours)
            } returns "Rewardable station-hours"
            every {
                context.getString(R.string.daily_tokens_to_be_rewarded)
            } returns "Daily tokens to be rewarded (max)"
            every {
                context.getString(R.string.total_tokens_to_be_rewarded)
            } returns "Total tokens to be rewarded (max)"
            every { context.getString(R.string.boost_period) } returns "Boost Period"
            every {
                context.getString(
                    R.string.boost_details_beta_desc,
                    any() as String,
                    any() as String
                )
            } returns "Boost details description"
            every { resources.getString(KEY_ANALYTICS) } returns "google_analytics"
            every { resources.getString(R.string.system_value) } returns "system_value"
            every { resources.getString(KEY_THEME) } returns "theme"
            every { sharedPref.getString("theme", "system_value") } returns "system_value"
        }

        override suspend fun afterProject() {
            println("Unit Tests completed!")
        }
    }
}
