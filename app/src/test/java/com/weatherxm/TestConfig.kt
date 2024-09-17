package com.weatherxm

import android.content.Context
import android.content.SharedPreferences
import com.weatherxm.data.Failure
import com.weatherxm.data.services.CacheService.Companion.KEY_PRECIP
import com.weatherxm.data.services.CacheService.Companion.KEY_PRESSURE
import com.weatherxm.data.services.CacheService.Companion.KEY_TEMPERATURE
import com.weatherxm.data.services.CacheService.Companion.KEY_WIND
import com.weatherxm.data.services.CacheService.Companion.KEY_WIND_DIR
import com.weatherxm.util.Resources
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.listeners.AfterProjectListener
import io.kotest.core.listeners.BeforeProjectListener
import io.kotest.core.names.DuplicateTestNameMode
import io.kotest.core.spec.AutoScan
import io.mockk.every
import io.mockk.mockk

object TestConfig : AbstractProjectConfig() {
    override val duplicateTestNameMode = DuplicateTestNameMode.Error
    val context = mockk<Context>()
    val resources = mockk<Resources>()
    val sharedPref = mockk<SharedPreferences>()
    val failure = mockk<Failure>()

    @AutoScan
    object MyProjectListener : BeforeProjectListener, AfterProjectListener {
        override suspend fun beforeProject() {
            every { resources.getString(R.string.uv_low) } returns "Low"
            every { resources.getString(R.string.uv_moderate) } returns "Moderate"
            every { resources.getString(R.string.uv_high) } returns "High"
            every { resources.getString(R.string.uv_very_high) } returns "Very High"
            every { resources.getString(R.string.uv_extreme) } returns "Extreme"
            every { resources.getString(KEY_TEMPERATURE) } returns "temperature_unit"
            every { context.getString(KEY_TEMPERATURE) } returns "temperature_unit"
            every { resources.getString(R.string.temperature_celsius) } returns "°C"
            every { context.getString(R.string.temperature_celsius) } returns "°C"
            every { resources.getString(R.string.degrees_mark) } returns "°"
            every { sharedPref.getString("temperature_unit", "°C") } returns "°C"
            every { resources.getString(R.string.solar_radiation_unit) } returns "W/m2"
            every { resources.getString(KEY_PRESSURE) } returns "key_pressure_preference"
            every { context.getString(KEY_PRESSURE) } returns "key_pressure_preference"
            every { resources.getString(R.string.pressure_hpa) } returns "hPa"
            every { context.getString(R.string.pressure_hpa) } returns "hPa"
            every { sharedPref.getString("key_pressure_preference", "hPa") } returns "hPa"
            every { resources.getString(KEY_PRECIP) } returns "precipitation_unit"
            every { context.getString(KEY_PRECIP) } returns "precipitation_unit"
            every { resources.getString(R.string.precipitation_mm) } returns "mm"
            every { context.getString(R.string.precipitation_mm) } returns "mm"
            every { resources.getString(R.string.precipitation_mm_hour) } returns "mm/h"
            every { resources.getString(R.string.precipitation_in) } returns "in"
            every { resources.getString(R.string.precipitation_in_hour) } returns "in/h"
            every { sharedPref.getString("precipitation_unit", "mm") } returns "mm"
            every { resources.getString(KEY_WIND) } returns "wind_speed_unit"
            every { context.getString(KEY_WIND) } returns "wind_speed_unit"
            every { resources.getString(R.string.wind_speed_ms) } returns "m/s"
            every { context.getString(R.string.wind_speed_ms) } returns "m/s"
            every { resources.getString(R.string.wind_speed_beaufort) } returns "bf"
            every { resources.getString(R.string.wind_speed_kmh) } returns "km/h"
            every { resources.getString(R.string.wind_speed_mph) } returns "mph"
            every { resources.getString(R.string.wind_speed_knots) } returns "knots"
            every { sharedPref.getString("wind_speed_unit", "m/s") } returns "m/s"
            every { resources.getString(KEY_WIND_DIR) } returns "key_wind_direction_preference"
            every { context.getString(KEY_WIND_DIR) } returns "key_wind_direction_preference"
            every { resources.getString(R.string.wind_direction_cardinal) } returns "Cardinal"
            every { context.getString(R.string.wind_direction_cardinal) } returns "Cardinal"
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
        }

        override suspend fun afterProject() {
            println("Unit Tests completed!")
        }
    }
}
