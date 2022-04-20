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

    fun getFormattedTemperature(value: Float?, decimals: Int = 0) = getFormattedValueOrEmpty(
        convertTemp(value), getPreferredUnit(
            resHelper.getString(R.string.key_temperature_preference),
            resHelper.getString(R.string.temperature_celsius)
        ), decimals
    )

    fun getFormattedPrecipitation(value: Float?) = getFormattedValueOrEmpty(
        convertPrecipitation(value), getPreferredUnit(
            resHelper.getString(R.string.key_precipitation_preference),
            resHelper.getString(R.string.precipitation_mm)
        ),
        getDecimalsPrecipitation()
    )

    fun getFormattedPrecipitationProbability(value: Int?) =
        getFormattedValueOrEmpty(value, "%")

    fun getFormattedHumidity(value: Int?) =
        getFormattedValueOrEmpty(value, "%")

    fun getFormattedPressure(value: Float?) = getFormattedValueOrEmpty(
        convertPressure(value), getPreferredUnit(
            resHelper.getString(R.string.key_pressure_preference),
            resHelper.getString(R.string.pressure_hpa)
        ),
        decimals = 1
    )

    fun getFormattedUV(value: Int?) =
        getFormattedValueOrEmpty(value, resHelper.getString(R.string.uv_index_unit))

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
            return "$value$windDegreesMark"
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
        unit: String,
        decimals: Int? = null
    ): String {
        if (value == null) {
            return EMPTY_VALUE
        }

        return if (decimals == null) {
            "$value$unit"
        } else {
            /*
            * Numbers in the range of -0.5..0 with 0 decimals show as -0.
            * So this is a custom fix to remove the "-" char and show them as 0.
             */
            if (willProduceNegativeZero(decimals, value)) {
                "%.${decimals}f$unit".format(value).replace("-", "")
            } else {
                "%.${decimals}f$unit".format(value)
            }
        }
    }

    /*
    * By using the .format() function with 0 decimals the numbers in the range of -0.5f..0f
    * display as -0 so we have this function to catch this case and act accordingly    *
     */
    @Suppress("MagicNumber")
    private fun willProduceNegativeZero(decimals: Int, value: Number): Boolean {
        val rangeOfNegativeZero = -0.5f..0f
        return decimals == 0 && value.toFloat() in rangeOfNegativeZero
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
