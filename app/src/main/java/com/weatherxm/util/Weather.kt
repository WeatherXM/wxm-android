package com.weatherxm.util

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.annotation.RawRes
import androidx.appcompat.content.res.AppCompatResources
import com.weatherxm.R
import com.weatherxm.data.services.CacheService.Companion.KEY_PRECIP
import com.weatherxm.data.services.CacheService.Companion.KEY_PRESSURE
import com.weatherxm.data.services.CacheService.Companion.KEY_TEMPERATURE
import com.weatherxm.data.services.CacheService.Companion.KEY_WIND
import com.weatherxm.data.services.CacheService.Companion.KEY_WIND_DIR
import com.weatherxm.ui.common.Contracts.EMPTY_VALUE
import com.weatherxm.ui.common.empty
import com.weatherxm.util.NumberUtils.roundToDecimals
import com.weatherxm.util.NumberUtils.roundToInt
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

@Suppress("TooManyFunctions")
object Weather : KoinComponent {

    private val resources: Resources by inject()
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
            "sleet" -> R.raw.anim_weather_sleet
            // The 3 following cases are for backward compatibility
            "wind" -> R.raw.anim_weather_wind
            "fog" -> R.raw.anim_weather_fog
            "cloudy" -> R.raw.anim_weather_cloudy
            else -> R.raw.anim_not_available
        }
    }

    /*
     * Suppress ComplexMethod because it is just a bunch of "when statements"
     */
    @Suppress("ComplexMethod")
    @RawRes
    fun getWeatherStaticIcon(icon: String?): Int? {
        return when (icon) {
            "clear-day" -> R.drawable.ic_weather_clear_day
            "clear-night" -> R.drawable.ic_weather_clear_night
            "partly-cloudy-day" -> R.drawable.ic_weather_partly_cloudy_day
            "partly-cloudy-night" -> R.drawable.ic_weather_partly_cloudy_night
            "overcast-day" -> R.drawable.ic_weather_overcast_day
            "overcast-night" -> R.drawable.ic_weather_overcast_night
            "drizzle" -> R.drawable.ic_weather_drizzle
            "rain" -> R.drawable.ic_weather_rain
            "thunderstorms-rain" -> R.drawable.ic_weather_thunderstorms_rain
            "snow" -> R.drawable.ic_weather_snow
            "sleet" -> R.drawable.ic_weather_sleet
            // The 3 following cases are for backward compatibility
            "wind" -> R.drawable.ic_weather_windy
            "fog" -> R.drawable.ic_weather_fog
            "cloudy" -> R.drawable.ic_weather_cloudy
            else -> null
        }
    }

    fun getFormattedTemperature(
        value: Float?,
        decimals: Int = 0,
        fullUnit: Boolean = true,
        includeUnit: Boolean = true,
        ignoreConversion: Boolean = false
    ): String {
        val unit = if (fullUnit && includeUnit) {
            getPreferredUnit(
                resources.getString(KEY_TEMPERATURE),
                resources.getString(R.string.temperature_celsius)
            )
        } else if (includeUnit) {
            resources.getString(R.string.degrees_mark)
        } else {
            String.empty()
        }

        if (value == null) {
            return "$EMPTY_VALUE$unit"
        }

        return if (ignoreConversion) {
            if (decimals == 0) {
                "${roundToInt(value)}$unit"
            } else {
                "${roundToDecimals(value)}$unit"
            }
        } else {
            "${convertTemp(value, decimals)}$unit"
        }
    }

    fun getFormattedPrecipitation(
        value: Float?,
        isRainRate: Boolean = true,
        includeUnit: Boolean = true,
        ignoreConversion: Boolean = false
    ): String {
        val unit = if (includeUnit) {
            getPrecipitationPreferredUnit(isRainRate)
        } else {
            String.empty()
        }

        if (value == null) {
            return "$EMPTY_VALUE$unit"
        }

        return if (ignoreConversion) {
            "${roundToDecimals(value, getDecimalsPrecipitation())}$unit"
        } else {
            "${convertPrecipitation(value)}$unit"
        }
    }

    fun getFormattedPrecipitationProbability(value: Int?, includeUnit: Boolean = true): String {
        val unit = if (includeUnit) {
            "%"
        } else {
            String.empty()
        }
        if (value == null) {
            return "$EMPTY_VALUE$unit"
        }

        return "$value$unit"
    }

    fun getFormattedHumidity(value: Int?, includeUnit: Boolean = true): String {
        val unit = if (includeUnit) {
            "%"
        } else {
            String.empty()
        }
        if (value == null) {
            return "$EMPTY_VALUE$unit"
        }

        return "$value$unit"
    }

    fun getFormattedUV(value: Int?, includeUnit: Boolean = true): String {
        if (value == null) {
            return EMPTY_VALUE
        }

        return if (includeUnit) {
            "$value${getUVClassification(value)}"
        } else {
            "$value"
        }
    }

    @Suppress("MagicNumber")
    fun getUVClassification(value: Int?): String {
        if (value == null) {
            return String.empty()
        }
        return when {
            value <= 2 -> resources.getString(R.string.uv_low)
            value <= 5 -> resources.getString(R.string.uv_moderate)
            value <= 7 -> resources.getString(R.string.uv_high)
            value <= 10 -> resources.getString(R.string.uv_very_high)
            else -> resources.getString(R.string.uv_extreme)
        }
    }

    fun getFormattedPressure(
        value: Float?,
        includeUnit: Boolean = true,
        ignoreConversion: Boolean = false
    ): String {
        val unit = if (includeUnit) {
            getPreferredUnit(
                resources.getString(KEY_PRESSURE),
                resources.getString(R.string.pressure_hpa)
            )
        } else {
            String.empty()
        }

        if (value == null) {
            return "$EMPTY_VALUE$unit"
        }

        return if (ignoreConversion) {
            "${roundToDecimals(value, getDecimalsPressure())}$unit"
        } else {
            "${convertPressure(value)}$unit"
        }
    }

    fun getFormattedSolarRadiation(value: Float?, includeUnit: Boolean = true): String {
        val unit = if (includeUnit) {
            resources.getString(R.string.solar_radiation_unit)
        } else {
            String.empty()
        }

        if (value == null) {
            return "$EMPTY_VALUE$unit"
        }

        return "${roundToDecimals(value)}$unit"
    }

    private fun getFormattedWindSpeed(
        value: Float?,
        includeUnit: Boolean = true,
        ignoreConversion: Boolean = false
    ): String {
        val unit = if (includeUnit) {
            getPreferredUnit(
                resources.getString(KEY_WIND),
                resources.getString(R.string.wind_speed_ms)
            )
        } else {
            String.empty()
        }
        if (value == null) {
            return "$EMPTY_VALUE$unit"
        }

        return if (ignoreConversion) {
            val beaufortUsed = unit == resources.getString(R.string.wind_speed_beaufort)
            if (beaufortUsed) {
                "${roundToInt(value)}$unit"
            } else {
                "${roundToDecimals(value)}$unit"
            }
        } else {
            "${convertWindSpeed(value)}$unit"
        }
    }

    fun getFormattedWindDirection(value: Int?): String {
        if (value == null) {
            return EMPTY_VALUE
        }
        val defaultUnit = resources.getString(R.string.wind_direction_cardinal)
        val savedUnit = sharedPref.getString(resources.getString(KEY_WIND_DIR), defaultUnit)

        return if (savedUnit != defaultUnit) {
            "$value${resources.getString(R.string.degrees_mark)}"
        } else {
            UnitConverter.degreesToCardinal(value)
        }
    }

    fun getFormattedWind(
        windSpeed: Float?,
        windDirection: Int?,
        includeUnits: Boolean = true,
        ignoreConversion: Boolean = false
    ): String {
        val windUnit = if (includeUnits) {
            getPreferredUnit(
                resources.getString(KEY_WIND), resources.getString(R.string.wind_speed_ms)
            )
        } else {
            String.empty()
        }

        return if (windSpeed != null && windDirection != null) {
            if (includeUnits) {
                "${getFormattedWindSpeed(windSpeed, ignoreConversion = ignoreConversion)} ${
                    getFormattedWindDirection(windDirection)
                }"
            } else {
                getFormattedWindSpeed(windSpeed, false, ignoreConversion = ignoreConversion)
            }
        } else "$EMPTY_VALUE$windUnit"
    }

    fun getWindDirectionDrawable(context: Context, index: Int?): Drawable? {
        return index?.let {
            val windDirectionDrawable = AppCompatResources.getDrawable(
                context, R.drawable.layers_wind_direction
            ) as LayerDrawable

            windDirectionDrawable.getDrawable(UnitConverter.getIndexOfCardinal(it))
        } ?: AppCompatResources.getDrawable(context, R.drawable.ic_weather_wind)
    }

    fun convertTemp(value: Number?, decimals: Int = 0): Number? {
        if (value == null) {
            Timber.d("Temperature value is null!")
            // Return null when value is null, so we catch it later on and show it as EMPTY
            return null
        }

        val defaultUnit = resources.getString(R.string.temperature_celsius)
        val savedUnit = sharedPref.getString(resources.getString(KEY_TEMPERATURE), defaultUnit)

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

        val defaultUnit = resources.getString(R.string.precipitation_mm)
        val savedUnit = sharedPref.getString(resources.getString(KEY_PRECIP), defaultUnit)

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

        val defaultUnit = resources.getString(R.string.wind_speed_ms)
        val savedUnit = sharedPref.getString(resources.getString(KEY_WIND), defaultUnit)

        // Return the value based on the weather unit the user wants
        return if (savedUnit != defaultUnit) {
            when (savedUnit) {
                resources.getString(R.string.wind_speed_knots) -> {
                    roundToDecimals(UnitConverter.msToKnots(value.toFloat()))
                }
                resources.getString(R.string.wind_speed_beaufort) -> {
                    UnitConverter.msToBeaufort(value.toFloat())
                }
                resources.getString(R.string.wind_speed_kmh) -> {
                    roundToDecimals(UnitConverter.msToKmh(value.toFloat()))
                }
                resources.getString(R.string.wind_speed_mph) -> {
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

        val defaultUnit = resources.getString(R.string.pressure_hpa)
        val savedUnit = sharedPref.getString(resources.getString(KEY_PRESSURE), defaultUnit)

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
        return sharedPref.getString(keyOnSharedPref, defaultUnit) ?: defaultUnit
    }

    fun getPrecipitationPreferredUnit(isRainRate: Boolean = true): String {
        val keyOnSharedPref = resources.getString(KEY_PRECIP)
        val defaultUnitOnPreferences = resources.getString(R.string.precipitation_mm)

        val savedUnit = getPreferredUnit(keyOnSharedPref, defaultUnitOnPreferences)

        return if (defaultUnitOnPreferences == savedUnit) {
            if (isRainRate) {
                resources.getString(R.string.precipitation_mm_hour)
            } else {
                resources.getString(R.string.precipitation_mm)
            }
        } else {
            if (isRainRate) {
                resources.getString(R.string.precipitation_in_hour)
            } else {
                resources.getString(R.string.precipitation_in)
            }
        }
    }

    fun getDecimalsPrecipitation(): Int {
        val keyOnSharedPref = resources.getString(KEY_PRECIP)
        val defaultUnit = resources.getString(R.string.precipitation_mm)

        val unit = getPreferredUnit(keyOnSharedPref, defaultUnit)

        return if (unit == defaultUnit) {
            DECIMALS_PRECIPITATION_MILLIMETERS
        } else {
            DECIMALS_PRECIPITATION_INCHES
        }
    }

    fun getDecimalsPressure(): Int {
        val unit = getPreferredUnit(
            resources.getString(KEY_PRESSURE), resources.getString(R.string.pressure_hpa)
        )

        return if (unit == resources.getString(R.string.pressure_hpa)) {
            DECIMALS_PRESSURE_HPA
        } else {
            DECIMALS_PRESSURE_INHG
        }
    }
}
