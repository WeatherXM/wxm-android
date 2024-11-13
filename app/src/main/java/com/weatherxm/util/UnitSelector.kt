package com.weatherxm.util

import android.content.Context
import com.weatherxm.R
import com.weatherxm.data.services.CacheService
import com.weatherxm.data.services.CacheService.Companion.KEY_PRECIP
import com.weatherxm.data.services.CacheService.Companion.KEY_PRESSURE
import com.weatherxm.data.services.CacheService.Companion.KEY_TEMPERATURE
import com.weatherxm.data.services.CacheService.Companion.KEY_WIND
import com.weatherxm.data.services.CacheService.Companion.KEY_WIND_DIR
import com.weatherxm.ui.common.WeatherUnit
import com.weatherxm.ui.common.WeatherUnitType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object UnitSelector : KoinComponent {
    private val cacheService: CacheService by inject()

    fun getTemperatureUnit(context: Context): WeatherUnit {
        val celsiusUnit = context.getString(R.string.temperature_celsius)
        val selectedUnit = cacheService.getPreferredUnit(
            unitKeyResId = KEY_TEMPERATURE,
            defaultUnitResId = R.string.temperature_celsius
        )

        return if (selectedUnit == celsiusUnit) {
            WeatherUnit(WeatherUnitType.CELSIUS, celsiusUnit)
        } else {
            WeatherUnit(WeatherUnitType.FAHRENHEIT, selectedUnit)
        }
    }

    fun getPrecipitationUnit(context: Context, isRainRate: Boolean): WeatherUnit {
        val millimetersUnit = context.getString(R.string.precipitation_mm)
        val millimetersPerHourUnit = context.getString(R.string.precipitation_mm_hour)
        val inchesUnit = context.getString(R.string.precipitation_in)
        val inchesPerHourUnit = context.getString(R.string.precipitation_in_hour)
        val selectedUnit = cacheService.getPreferredUnit(
            unitKeyResId = KEY_PRECIP,
            defaultUnitResId = R.string.precipitation_mm
        )
        return if (selectedUnit == millimetersUnit && isRainRate) {
            WeatherUnit(WeatherUnitType.MILLIMETERS, millimetersPerHourUnit)
        } else if (selectedUnit == millimetersUnit) {
            WeatherUnit(WeatherUnitType.MILLIMETERS, millimetersUnit)
        } else if (isRainRate) {
            WeatherUnit(WeatherUnitType.INCHES, inchesPerHourUnit)
        } else {
            WeatherUnit(WeatherUnitType.INCHES, inchesUnit)
        }
    }

    fun getPressureUnit(context: Context): WeatherUnit {
        val hPaUnit = context.getString(R.string.pressure_hpa)
        val selectedUnit = cacheService.getPreferredUnit(
            unitKeyResId = KEY_PRESSURE,
            defaultUnitResId = R.string.key_pressure_preference
        )
        return if (selectedUnit == hPaUnit) {
            WeatherUnit(WeatherUnitType.HPA, hPaUnit)
        } else {
            WeatherUnit(WeatherUnitType.INHG, selectedUnit)
        }
    }

    fun getWindUnit(context: Context): WeatherUnit {
        val msUnit = context.getString(R.string.wind_speed_ms)
        val beaufortUnit = context.getString(R.string.wind_speed_beaufort)
        val knotsUnit = context.getString(R.string.wind_speed_knots)
        val mphUnit = context.getString(R.string.wind_speed_mph)
        val kmhUnit = context.getString(R.string.wind_speed_kmh)
        val selectedUnit = cacheService.getPreferredUnit(
            unitKeyResId = KEY_WIND,
            defaultUnitResId = R.string.wind_speed_ms
        )

        return when (selectedUnit) {
            msUnit -> WeatherUnit(WeatherUnitType.MS, msUnit)
            beaufortUnit -> WeatherUnit(WeatherUnitType.BEAUFORT, beaufortUnit)
            knotsUnit -> WeatherUnit(WeatherUnitType.KNOTS, knotsUnit)
            mphUnit -> WeatherUnit(WeatherUnitType.MPH, mphUnit)
            kmhUnit -> WeatherUnit(WeatherUnitType.KMH, kmhUnit)
            else -> WeatherUnit(WeatherUnitType.MS, msUnit)
        }
    }

    fun getWindDirectionUnit(context: Context): WeatherUnit {
        val cardinalUnit = context.getString(R.string.wind_direction_cardinal)
        val selectedUnit = cacheService.getPreferredUnit(
            unitKeyResId = KEY_WIND_DIR,
            defaultUnitResId = R.string.wind_direction_cardinal
        )
        return if (selectedUnit == cardinalUnit) {
            WeatherUnit(WeatherUnitType.CARDINAL, cardinalUnit)
        } else {
            WeatherUnit(WeatherUnitType.DEGREES, selectedUnit)
        }
    }

}


