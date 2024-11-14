package com.weatherxm.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.annotation.RawRes
import androidx.appcompat.content.res.AppCompatResources
import com.weatherxm.R
import com.weatherxm.ui.common.Contracts.DEGREES_MARK
import com.weatherxm.ui.common.Contracts.EMPTY_VALUE
import com.weatherxm.ui.common.WeatherUnitType
import com.weatherxm.ui.common.empty
import com.weatherxm.util.NumberUtils.formatNumber
import com.weatherxm.util.NumberUtils.roundToDecimals
import org.koin.core.component.KoinComponent
import timber.log.Timber

@Suppress("TooManyFunctions")
object Weather : KoinComponent {
    private const val DECIMALS_PRECIPITATION_INCHES = 2
    private const val DECIMALS_PRECIPITATION_MILLIMETERS = 1
    private const val DECIMALS_PRESSURE_INHG = 2
    private const val DECIMALS_PRESSURE_HPA = 1

    /**
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

    /**
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
        context: Context,
        value: Float?,
        decimals: Int = 0,
        fullUnit: Boolean = true,
        includeUnit: Boolean = true,
        ignoreConversion: Boolean = false
    ): String {
        val weatherUnit = UnitSelector.getTemperatureUnit(context)
        val unit = if (fullUnit && includeUnit) {
            weatherUnit.unit
        } else if (includeUnit) {
            DEGREES_MARK
        } else {
            String.empty()
        }

        if (value == null) {
            return "$EMPTY_VALUE$unit"
        }

        val formattedValue = if (ignoreConversion) {
            formatNumber(value, decimals)
        } else {
            formatNumber(convertTemp(context, value, decimals), decimals)
        }

        return "$formattedValue$unit"
    }

    fun getFormattedPrecipitation(
        context: Context,
        value: Float?,
        isRainRate: Boolean = true,
        includeUnit: Boolean = true,
        ignoreConversion: Boolean = false
    ): String {
        val weatherUnit = UnitSelector.getPrecipitationUnit(context, isRainRate)
        val unit = if (includeUnit) {
            weatherUnit.unit
        } else {
            String.empty()
        }

        if (value == null) {
            return "$EMPTY_VALUE$unit"
        }

        val formattedValue = if (ignoreConversion) {
            formatNumber(value, getDecimalsPrecipitation(weatherUnit.type))
        } else {
            formatNumber(
                convertPrecipitation(context, value),
                getDecimalsPrecipitation(weatherUnit.type)
            )
        }

        return "$formattedValue$unit"
    }

    fun getFormattedPrecipitationProbability(value: Int?, includeUnit: Boolean = true): String {
        val unit = if (includeUnit) "%" else String.empty()
        return if (value == null) {
            "$EMPTY_VALUE$unit"
        } else {
            "$value$unit"
        }
    }

    fun getFormattedHumidity(value: Int?, includeUnit: Boolean = true): String {
        val unit = if (includeUnit) "%" else String.empty()
        return if (value == null) {
            "$EMPTY_VALUE$unit"
        } else {
            "$value$unit"
        }
    }

    fun getFormattedUV(context: Context, value: Int?, includeUnit: Boolean = true): String {
        return if (value == null) {
            EMPTY_VALUE
        } else if (includeUnit) {
            "$value${getUVClassification(context, value)}"
        } else {
            "$value"
        }
    }

    @Suppress("MagicNumber")
    fun getUVClassification(context: Context, value: Int?): String {
        return if (value == null) {
            String.empty()
        } else {
            when {
                value <= 2 -> context.getString(R.string.uv_low)
                value <= 5 -> context.getString(R.string.uv_moderate)
                value <= 7 -> context.getString(R.string.uv_high)
                value <= 10 -> context.getString(R.string.uv_very_high)
                else -> context.getString(R.string.uv_extreme)
            }
        }
    }

    fun getFormattedPressure(
        context: Context,
        value: Float?,
        includeUnit: Boolean = true,
        ignoreConversion: Boolean = false
    ): String {
        val weatherUnit = UnitSelector.getPressureUnit(context)
        val unit = if (includeUnit) {
            weatherUnit.unit
        } else {
            String.empty()
        }

        if (value == null) {
            return "$EMPTY_VALUE$unit"
        }

        val formattedValue = if (ignoreConversion) {
            formatNumber(value, getDecimalsPressure(weatherUnit.type))
        } else {
            formatNumber(convertPressure(context, value), getDecimalsPressure(weatherUnit.type))
        }

        return "$formattedValue$unit"
    }

    fun getFormattedSolarRadiation(
        context: Context,
        value: Float?,
        includeUnit: Boolean = true
    ): String {
        val unit = if (includeUnit) {
            context.getString(R.string.solar_radiation_unit)
        } else {
            String.empty()
        }
        return if (value == null) {
            "$EMPTY_VALUE$unit"
        } else {
            "${formatNumber(value, 1)}$unit"
        }
    }

    private fun getFormattedWindSpeed(
        context: Context,
        value: Float?,
        includeUnit: Boolean = true,
        ignoreConversion: Boolean = false
    ): String {
        val weatherUnit = UnitSelector.getWindUnit(context)
        val unit = if (includeUnit) {
            weatherUnit.unit
        } else {
            String.empty()
        }
        if (value == null) {
            return "$EMPTY_VALUE$unit"
        }

        val decimals = if (weatherUnit.type == WeatherUnitType.BEAUFORT) {
            0
        } else {
            1
        }

        val formattedValue = if (ignoreConversion) {
            formatNumber(value, decimals)
        } else {
            formatNumber(convertWindSpeed(context, value), decimals)
        }

        return "$formattedValue$unit"
    }

    fun getFormattedWindDirection(context: Context, value: Int?): String {
        if (value == null) {
            return EMPTY_VALUE
        }

        val weatherUnit = UnitSelector.getWindDirectionUnit(context)
        return if (weatherUnit.type == WeatherUnitType.DEGREES) {
            "$value$DEGREES_MARK"
        } else {
            UnitConverter.degreesToCardinal(value)
        }
    }

    fun getFormattedWind(
        context: Context,
        windSpeed: Float?,
        windDirection: Int?,
        includeUnits: Boolean = true,
        ignoreConversion: Boolean = false
    ): String {
        val windUnit = if (includeUnits) {
            UnitSelector.getWindUnit(context).unit
        } else {
            String.empty()
        }

        return if (windSpeed != null && windDirection != null) {
            val formattedWindSpeed = getFormattedWindSpeed(
                context = context,
                value = windSpeed,
                includeUnit = includeUnits,
                ignoreConversion = ignoreConversion
            )
            if (includeUnits) {
                "$formattedWindSpeed ${getFormattedWindDirection(context, windDirection)}"
            } else {
                formattedWindSpeed
            }
        } else {
            "$EMPTY_VALUE$windUnit"
        }
    }

    fun getWindDirectionDrawable(context: Context, index: Int?): Drawable? {
        return index?.let {
            val windDirectionDrawable = AppCompatResources.getDrawable(
                context, R.drawable.layers_wind_direction
            ) as LayerDrawable

            windDirectionDrawable.getDrawable(UnitConverter.getIndexOfCardinal(it))
        } ?: AppCompatResources.getDrawable(context, R.drawable.ic_weather_wind)
    }

    fun convertTemp(context: Context, value: Number, decimals: Int = 0): Number {
        // Return the value based on the weather unit the user wants
        return if (UnitSelector.getTemperatureUnit(context).type == WeatherUnitType.CELSIUS) {
            roundToDecimals(value, decimals)
        } else {
            roundToDecimals(UnitConverter.celsiusToFahrenheit(value.toFloat()), decimals)
        }
    }

    fun convertPrecipitation(context: Context, value: Number?): Number? {
        if (value == null) {
            Timber.d("Precipitation value is null!")
            // Return null when value is null, so we catch it later on and show it as EMPTY
            return null
        }

        // Return the value based on the weather unit the user wants
        val precipUnitType = UnitSelector.getPrecipitationUnit(context, false).type
        return if (precipUnitType == WeatherUnitType.MILLIMETERS) {
            // This is the default value - millimeters - so we show 1 decimal
            roundToDecimals(value)
        } else {
            // On inches we use 2 decimals
            roundToDecimals(UnitConverter.millimetersToInches(value.toFloat()), decimals = 2)
        }
    }

    fun convertWindSpeed(context: Context, value: Number?): Number? {
        if (value == null) {
            Timber.d("Wind speed value is null!")
            // Return null when value is null, so we catch it later on and show it as EMPTY
            return null
        }

        // Return the value based on the weather unit the user wants
        return when (UnitSelector.getWindUnit(context).type) {
            WeatherUnitType.MS -> {
                roundToDecimals(value)
            }
            WeatherUnitType.KMH -> {
                roundToDecimals(UnitConverter.msToKmh(value.toFloat()))
            }
            WeatherUnitType.MPH -> {
                roundToDecimals(UnitConverter.msToMph(value.toFloat()))
            }
            WeatherUnitType.KNOTS -> {
                roundToDecimals(UnitConverter.msToKnots(value.toFloat()))
            }
            WeatherUnitType.BEAUFORT -> {
                UnitConverter.msToBeaufort(value.toFloat())
            }
            else -> {
                // This is the default value - m/s - so we show 1 decimal
                roundToDecimals(value)
            }
        }
    }

    fun convertPressure(context: Context, value: Number?): Number? {
        if (value == null) {
            Timber.d("Pressure value is null!")
            // Return null when value is null, so we catch it later on and show it as EMPTY
            return null
        }

        // Return the value based on the weather unit the user wants
        return if (UnitSelector.getPressureUnit(context).type == WeatherUnitType.HPA) {
            // This is the default value - hpa - so we show 1 decimal
            roundToDecimals(value)
        } else {
            // On inHg we use 2 decimals
            roundToDecimals(UnitConverter.hpaToInHg(value.toFloat()), decimals = 2)
        }
    }

    fun getDecimalsPrecipitation(unitType: WeatherUnitType): Int {
        return if (unitType == WeatherUnitType.MILLIMETERS) {
            DECIMALS_PRECIPITATION_MILLIMETERS
        } else {
            DECIMALS_PRECIPITATION_INCHES
        }
    }

    fun getDecimalsPressure(unitType: WeatherUnitType): Int {
        return if (unitType == WeatherUnitType.HPA) {
            DECIMALS_PRESSURE_HPA
        } else {
            DECIMALS_PRESSURE_INHG
        }
    }
}
