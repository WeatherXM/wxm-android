package com.weatherxm.util

import android.content.SharedPreferences
import androidx.annotation.RawRes
import com.weatherxm.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.math.BigDecimal

@Suppress("TooManyFunctions")
object Weather : KoinComponent {

    private const val EMPTY_VALUE = "-"
    private val resHelper: ResourcesHelper by inject()
    private val sharedPref: SharedPreferences by inject()

    private const val DECIMALS_PRECIPITATION_INCHES = 2
    private const val DECIMALS_PRECIPITATION_MILLIMETERS = 1
    private const val DECIMALS_PRESSURE_INHG = 2
    private const val DECIMALS_PRESSURE_HPA = 1

    /*
     * Suppress ComplexMethod because it is just a bunch of "when statements"
     */
    @Suppress("ComplexMethod")
    @RawRes
    fun getWeatherAnimation(icon: String?): Int {
        return when (icon) {
            "not-available" -> R.raw.anim_not_available
            "clear-day" -> R.raw.anim_weather_clear_day
            "clear-night" -> R.raw.anim_weather_clear_night
            "partly-cloudy-day" -> R.raw.anim_weather_partly_cloudy_day
            "partly-cloudy-night" -> R.raw.anim_weather_partly_cloudy_night
            "overcast-day" -> R.raw.anim_weather_overcast_day
            "overcast-night" -> R.raw.anim_weather_overcast_night
            "drizzle" -> R.raw.anim_weather_drizzle
            "rain" -> R.raw.anim_weather_rain
            "thunderstorms-rain" -> R.raw.anim_weather_thunderstorms_rain
            "snow" -> R.raw.anim_weather_snow
            "sleet" -> R.raw.anim_weather_snow
            // The 3 following cases are for backward compatibility
            "wind" -> R.raw.anim_weather_wind
            "fog" -> R.raw.anim_weather_fog
            "cloudy" -> R.raw.anim_weather_cloudy
            else -> R.raw.anim_not_available
        }
    }

    fun getFormattedTemperature(value: Float?, decimals: Int = 0): String {
        if (value == null) {
            return EMPTY_VALUE
        }

        val valueToReturn = convertTemp(value, decimals)
        val unit = getPreferredUnit(
            resHelper.getString(R.string.key_temperature_preference),
            resHelper.getString(R.string.temperature_celsius)
        )

        return "$valueToReturn$unit"
    }

    fun getFormattedPrecipitation(value: Float?): String {
        if (value == null) {
            return EMPTY_VALUE
        }

        val valueToReturn = convertPrecipitation(value)
        val unit = getPreferredUnit(
            resHelper.getString(R.string.key_precipitation_preference),
            resHelper.getString(R.string.precipitation_mm)
        )

        return "$valueToReturn$unit"
    }

    fun getFormattedPrecipitationProbability(value: Int?): String {
        if (value == null) {
            return EMPTY_VALUE
        }

        return "$value%"
    }

    fun getFormattedHumidity(value: Int?): String {
        if (value == null) {
            return EMPTY_VALUE
        }

        return "$value%"
    }

    fun getFormattedUV(value: Int?): String {
        if (value == null) {
            return EMPTY_VALUE
        }

        val unit = resHelper.getString(R.string.uv_index_unit)

        return "$value$unit"
    }

    fun getFormattedPressure(value: Float?): String {
        if (value == null) {
            return EMPTY_VALUE
        }

        val valueToReturn = convertPressure(value)
        val unit = getPreferredUnit(
            resHelper.getString(R.string.key_pressure_preference),
            resHelper.getString(R.string.pressure_hpa)
        )

        return "$valueToReturn$unit"
    }

    private fun getFormattedWindSpeed(value: Float?): String {
        if (value == null) {
            return EMPTY_VALUE
        }

        val valueToReturn = convertWindSpeed(value)
        val unit = getPreferredUnit(
            resHelper.getString(R.string.key_wind_speed_preference),
            resHelper.getString(R.string.wind_speed_ms)
        )

        return "$valueToReturn$unit"
    }

    fun getFormattedWindDirection(value: Int): String {
        val defaultUnit = resHelper.getString(R.string.wind_direction_cardinal)
        val savedUnit =
            sharedPref.getString(
                resHelper.getString(R.string.key_wind_direction_preference),
                defaultUnit
            )

        if (savedUnit != defaultUnit) {
            val windDegreesMark = resHelper.getString(R.string.wind_direction_degrees_mark)
            return "$value$windDegreesMark"
        }

        return UnitConverter.degreesToCardinal(value)
    }

    fun getFormattedWind(windSpeed: Float?, windDirection: Int?): String {
        return if (windSpeed != null && windDirection != null) {
            "${getFormattedWindSpeed(windSpeed)} ${getFormattedWindDirection(windDirection)}"
        } else EMPTY_VALUE
    }

    fun convertTemp(value: Number?, decimals: Int = 0): Number? {
        if (value == null) {
            Timber.d("Temperature value is null!")
            // Return null when value is null, so we catch it later on and show it as EMPTY
            return null
        }

        val defaultUnit = resHelper.getString(R.string.temperature_celsius)
        val savedUnit =
            sharedPref.getString(
                resHelper.getString(R.string.key_temperature_preference),
                defaultUnit
            )

        // Return the value based on the weather unit the user wants
        val valueToReturn = if (savedUnit != defaultUnit) {
            UnitConverter.celsiusToFahrenheit(value.toFloat())
        } else {
            value
        }

        return if (decimals == 0) {
            roundToInt(valueToReturn)
        } else {
            roundToDecimals(valueToReturn)
        }
    }

    fun convertPrecipitation(value: Number?): Number? {
        if (value == null) {
            Timber.d("Precipitation value is null!")
            // Return null when value is null, so we catch it later on and show it as EMPTY
            return null
        }

        val defaultUnit = resHelper.getString(R.string.precipitation_mm)
        val savedUnit =
            sharedPref.getString(
                resHelper.getString(R.string.key_precipitation_preference),
                defaultUnit
            )

        // Return the value based on the weather unit the user wants
        return if (savedUnit != defaultUnit) {
            // On inches we use 2 decimals
            roundToDecimals(UnitConverter.millimetersToInches(value.toFloat()), decimals = 2)
        } else {
            // This is the default value - millimeters - so we show 1 decimal
            roundToDecimals(value)
        }
    }

    fun convertWindSpeed(value: Number?): Number? {
        if (value == null) {
            Timber.d("Wind speed value is null!")
            // Return null when value is null, so we catch it later on and show it as EMPTY
            return null
        }

        val defaultUnit = resHelper.getString(R.string.wind_speed_ms)
        val savedUnit =
            sharedPref.getString(
                resHelper.getString(R.string.key_wind_speed_preference),
                defaultUnit
            )

        // Return the value based on the weather unit the user wants
        return if (savedUnit != defaultUnit) {
            when (savedUnit) {
                resHelper.getString(R.string.wind_speed_knots) -> {
                    roundToDecimals(UnitConverter.msToKnots(value.toFloat()))
                }
                resHelper.getString(R.string.wind_speed_beaufort) -> {
                    UnitConverter.msToBeaufort(value.toFloat())
                }
                resHelper.getString(R.string.wind_speed_kmh) -> {
                    roundToDecimals(UnitConverter.msToKmh(value.toFloat()))
                }
                resHelper.getString(R.string.wind_speed_mph) -> {
                    roundToDecimals(UnitConverter.msToMph(value.toFloat()))
                }
                else -> {
                    null
                }
            }
        } else {
            // This is the default value - m/s - so we show 1 decimal
            roundToDecimals(value)
        }
    }

    fun convertPressure(value: Number?): Number? {
        if (value == null) {
            Timber.d("Pressure value is null!")
            // Return null when value is null, so we catch it later on and show it as EMPTY
            return null
        }

        val defaultUnit = resHelper.getString(R.string.pressure_hpa)
        val savedUnit =
            sharedPref.getString(resHelper.getString(R.string.key_pressure_preference), defaultUnit)

        // Return the value based on the weather unit the user wants
        return if (savedUnit != defaultUnit) {
            // On inHg we use 2 decimals
            roundToDecimals(UnitConverter.hpaToInHg(value.toFloat()), decimals = 2)
        } else {
            // This is the default value - hpa - so we show 1 decimal
            roundToDecimals(value)
        }
    }

    fun getPreferredUnit(keyOnSharedPref: String, defaultUnit: String): String {
        val savedUnit = sharedPref.getString(keyOnSharedPref, "")
        if (!savedUnit.isNullOrEmpty()) {
            return savedUnit
        }
        return defaultUnit
    }

    fun getDecimalsPrecipitation(): Int {
        val keyOnSharedPref = resHelper.getString(R.string.key_precipitation_preference)
        val defaultUnit = resHelper.getString(R.string.precipitation_mm)

        val unit = getPreferredUnit(keyOnSharedPref, defaultUnit)

        return if (unit == resHelper.getString(R.string.precipitation_mm)) {
            DECIMALS_PRECIPITATION_MILLIMETERS
        } else {
            DECIMALS_PRECIPITATION_INCHES
        }
    }

    fun getDecimalsPressure(): Int {
        val keyOnSharedPref = resHelper.getString(R.string.key_pressure_preference)
        val defaultUnit = resHelper.getString(R.string.pressure_hpa)

        val unit = getPreferredUnit(keyOnSharedPref, defaultUnit)

        return if (unit == resHelper.getString(R.string.pressure_hpa)) {
            DECIMALS_PRESSURE_HPA
        } else {
            DECIMALS_PRESSURE_INHG
        }
    }

    fun roundToDecimals(value: Number, decimals: Int = 1): Float {
        return value.toFloat().toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP).toFloat()
    }

    fun roundToInt(value: Number): Int {
        return value.toFloat().toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).toInt()
    }
}
