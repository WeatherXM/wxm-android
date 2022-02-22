package com.weatherxm.util

import android.content.SharedPreferences
import androidx.annotation.RawRes
import com.weatherxm.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

@Suppress("TooManyFunctions")
object Weather : KoinComponent {

    private const val EMPTY_VALUE = "-"
    private val resHelper: ResourcesHelper by inject()
    private val sharedPref: SharedPreferences by inject()

    private const val DECIMALS_WIND_SPEED = 1
    private const val DECIMALS_PRECIPITATION_INCHES = 2
    private const val DECIMALS_PRECIPITATION_MILLIMETERS = 1

    @RawRes
    fun getWeatherAnimation(icon: String?): Int {
        return when (icon) {
            "clear-day" -> R.raw.anim_weather_clear_day
            "clear-night" -> R.raw.anim_weather_clear_night
            "rain" -> R.raw.anim_weather_rain
            "snow" -> R.raw.anim_weather_snow
            "sleet" -> R.raw.anim_weather_snow
            "wind" -> R.raw.anim_weather_wind
            "fog" -> R.raw.anim_weather_fog
            "cloudy" -> R.raw.anim_weather_cloudy
            "partly-cloudy-day" -> R.raw.anim_weather_partly_cloudy_day
            "partly-cloudy-night" -> R.raw.anim_weather_partly_cloudy_night
            else -> R.raw.anim_weather_none
        }
    }

    fun getFormattedTemperature(value: Float?) = getFormattedValueOrEmpty(
        convertTemp(value), getPreferredUnit(
            resHelper.getString(R.string.key_temperature_preference),
            resHelper.getString(R.string.temperature_celsius)
        ), 0
    )

    fun getFormattedPrecipitation(value: Float?) = getFormattedValueOrEmpty(
        convertPrecipitation(value), getPreferredUnit(
            resHelper.getString(R.string.key_precipitation_preference),
            resHelper.getString(R.string.precipitation_mm)
        ),
        getDecimalsPrecipitation()
    )

    fun getFormattedPrecipitationProbability(value: Int?) = getFormattedValueOrEmpty(value, "%")

    fun getFormattedHumidity(value: Int?) = getFormattedValueOrEmpty(value, "%")

    fun getFormattedPressure(value: Float?) = getFormattedValueOrEmpty(
        convertPressure(value), getPreferredUnit(
            resHelper.getString(R.string.key_pressure_preference),
            resHelper.getString(R.string.pressure_hpa)
        ), 1
    )

    fun getFormattedCloud(value: Int?) = getFormattedValueOrEmpty(value, "%")

    fun getFormattedUV(value: Int?) = getFormattedValueOrEmpty(value, "UV")

    private fun getFormattedWindSpeed(value: Float?) = getFormattedValueOrEmpty(
        convertWindSpeed(value),
        getPreferredUnit(
            resHelper.getString(R.string.key_wind_speed_preference),
            resHelper.getString(R.string.wind_speed_ms)
        ),
        getDecimalsWindSpeed()
    )

    fun getFormattedWindDirection(value: Int): String {
        val windPreferenceKey = resHelper.getString(R.string.key_wind_direction_preference)
        val windPreferenceDefValue = resHelper.getString(R.string.wind_direction_cardinal)
        val savedPreferenceUnit = sharedPref.getString(windPreferenceKey, "")

        if (!savedPreferenceUnit.isNullOrEmpty() && savedPreferenceUnit != windPreferenceDefValue) {
            val windDegreesMark = resHelper.getString(R.string.wind_direction_degrees_mark)
            return "$value $windDegreesMark"
        }

        return UnitConverter.degreesToCardinal(value)
    }

    fun getFormattedWind(windSpeed: Float?, windDirection: Int?): String {
        return if (windSpeed != null && windDirection != null) {
            "${getFormattedWindSpeed(windSpeed)} ${getFormattedWindDirection(windDirection)}"
        } else EMPTY_VALUE
    }

    fun getFormattedValueOrEmpty(
        value: Number?,
        units: String,
        decimals: Int? = null
    ): String {
        if (value == null) {
            return EMPTY_VALUE
        }
        return if (decimals == null) {
            "$value$units"
        } else {
            "%.${decimals}f$units".format(value)
        }
    }

    fun convertTemp(value: Number?): Number? {
        if (value == null) {
            Timber.d("Temperature value is null!")
            // Return null when value is null, so we catch it later on and show it as EMPTY
            return null
        }

        // Default value
        var valueToReturn: Number = value
        val savedUnit =
            sharedPref.getString(resHelper.getString(R.string.key_temperature_preference), "")
        val defaultUnit = resHelper.getString(R.string.temperature_celsius)
        // We need to convert the value as different preference than default is used
        if (!savedUnit.isNullOrEmpty() && savedUnit != defaultUnit) {
            valueToReturn = UnitConverter.celsiusToFahrenheit(value.toFloat())
        }
        return valueToReturn
    }

    fun convertPrecipitation(value: Number?): Number? {
        if (value == null) {
            Timber.d("Precipitation value is null!")
            // Return null when value is null, so we catch it later on and show it as EMPTY
            return null
        }

        // Default value
        var valueToReturn: Number = value
        val savedUnit =
            sharedPref.getString(resHelper.getString(R.string.key_precipitation_preference), "")
        val defaultUnit = resHelper.getString(R.string.precipitation_mm)
        // We need to convert the value as different preference than default is used
        if (!savedUnit.isNullOrEmpty() && savedUnit != defaultUnit) {
            valueToReturn = UnitConverter.millimetersToInches(value.toFloat())
        }
        return valueToReturn
    }

    fun convertWindSpeed(value: Number?): Number? {
        if (value == null) {
            Timber.d("Wind speed value is null!")
            // Return null when value is null, so we catch it later on and show it as EMPTY
            return null
        }

        var valueToReturn: Number = value
        val savedUnit =
            sharedPref.getString(resHelper.getString(R.string.key_wind_speed_preference), "")
        val defaultUnit = resHelper.getString(R.string.wind_speed_ms)
        // We need to convert the value as different preference than default is used
        if (!savedUnit.isNullOrEmpty() && savedUnit != defaultUnit) {
            when (savedUnit) {
                resHelper.getString(R.string.wind_speed_knots) -> {
                    valueToReturn = UnitConverter.msToKnots(value.toFloat())

                }
                resHelper.getString(R.string.wind_speed_beaufort) -> {
                    valueToReturn = UnitConverter.msToBeaufort(value.toFloat())

                }
                resHelper.getString(R.string.wind_speed_kmh) -> {
                    valueToReturn = UnitConverter.msToKmh(value.toFloat())
                }
                resHelper.getString(R.string.wind_speed_mph) -> {
                    valueToReturn = UnitConverter.msToMph(value.toFloat())
                }
            }
        }
        return valueToReturn
    }

    fun convertPressure(value: Number?): Number? {
        if (value == null) {
            Timber.d("Pressure value is null!")
            // Return null when value is null, so we catch it later on and show it as EMPTY
            return null
        }

        // Default value
        var valueToReturn: Number = value
        val savedUnit =
            sharedPref.getString(resHelper.getString(R.string.key_pressure_preference), "")
        val defaultUnit = resHelper.getString(R.string.pressure_hpa)
        // We need to convert the value as different preference than default is used
        if (!savedUnit.isNullOrEmpty() && savedUnit != defaultUnit) {
            valueToReturn = UnitConverter.hpaToInHg(value.toFloat())
        }
        return valueToReturn
    }

    fun getPreferredUnit(keyOnSharedPref: String, defaultUnit: String): String {
        val savedUnit = sharedPref.getString(keyOnSharedPref, "")
        if (!savedUnit.isNullOrEmpty()) {
            return savedUnit
        }
        return defaultUnit
    }

    fun getDecimalsWindSpeed(): Int? {
        val keyOnSharedPref = resHelper.getString(R.string.key_wind_speed_preference)
        val defaultUnit = resHelper.getString(R.string.wind_speed_ms)

        val unit = getPreferredUnit(keyOnSharedPref, defaultUnit)

        if (unit == resHelper.getString(R.string.wind_speed_beaufort)) {
            // Return null when bf units are used so we show no decimals at all.
            return null
        }

        return DECIMALS_WIND_SPEED
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
}
