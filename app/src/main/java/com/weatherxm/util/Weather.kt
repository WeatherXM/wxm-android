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

@Suppress("TooManyFunctions")
object Weather {
    private const val DECIMALS_PRECIPITATION_INCHES = 2
    private const val DECIMALS_PRECIPITATION_MILLIMETERS = 1
    private const val DECIMALS_PRESSURE_INHG = 2
    private const val DECIMALS_PRESSURE_HPA = 1
    private const val DECIMALS_WIND_DEFAULT = 1
    private const val DECIMALS_WIND_BEAUFORT = 0

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
            "partly-cloudy-day-drizzle" -> R.raw.anim_weather_partly_cloudy_day_drizzle
            "partly-cloudy-night-drizzle" -> R.raw.anim_weather_partly_cloudy_night_drizzle
            "partly-cloudy-day-snow" -> R.raw.anim_weather_partly_cloudy_day_snow
            "partly-cloudy-night-snow" -> R.raw.anim_weather_partly_cloudy_night_snow
            "overcast-day" -> R.raw.anim_weather_overcast_day
            "overcast-night" -> R.raw.anim_weather_overcast_night
            "overcast" -> R.raw.anim_weather_overcast
            "overcast-rain" -> R.raw.anim_weather_overcast_rain
            "overcast-snow" -> R.raw.anim_weather_overcast_snow
            "overcast-drizzle" -> R.raw.anim_weather_overcast_drizzle
            "overcast-light-snow" -> R.raw.anim_weather_overcast_light_snow
            "overcast-sleet" -> R.raw.anim_weather_overcast_sleet
            "drizzle" -> R.raw.anim_weather_drizzle
            "rain" -> R.raw.anim_weather_rain
            "thunderstorms-rain" -> R.raw.anim_weather_thunderstorms_rain
            "thunderstorms-overcast-rain" -> R.raw.anim_weather_thunderstorms_overcast_rain
            "thunderstorms-light-rain" -> R.raw.anim_weather_thunderstorms_light_rain
            "thunderstorms-extreme-rain" -> R.raw.anim_weather_thunderstorms_extreme_rain
            "snow" -> R.raw.anim_weather_snow
            "sleet" -> R.raw.anim_weather_sleet
            "haze-day" -> R.raw.anim_weather_haze_day
            "haze-night" -> R.raw.anim_weather_haze_night
            "extreme-day" -> R.raw.anim_weather_extreme_day
            "extreme-night" -> R.raw.anim_weather_extreme_night
            "extreme-rain" -> R.raw.anim_weather_extreme_rain
            "extreme-snow" -> R.raw.anim_weather_extreme_snow
            "extreme-day-rain" -> R.raw.anim_weather_extreme_day_rain
            "extreme-night-rain" -> R.raw.anim_weather_extreme_night_rain
            "extreme-day-sleet" -> R.raw.anim_weather_extreme_day_sleet
            "extreme-night-sleet" -> R.raw.anim_weather_extreme_night_sleet
            "extreme-day-snow" -> R.raw.anim_weather_extreme_day_snow
            "extreme-night-snow" -> R.raw.anim_weather_extreme_night_snow
            "extreme-day-drizzle" -> R.raw.anim_weather_extreme_day_drizzle
            "extreme-night-drizzle" -> R.raw.anim_weather_extreme_night_drizzle
            "extreme-day-light-snow" -> R.raw.anim_weather_extreme_day_light_snow
            "extreme-night-light-snow" -> R.raw.anim_weather_extreme_night_light_snow
            "dust-day" -> R.raw.anim_weather_dust_day
            "dust-night" -> R.raw.anim_weather_dust_night
            "dust-wind" -> R.raw.anim_weather_dust_wind
            // The 3 following cases are for backward compatibility
            "wind" -> R.raw.anim_weather_wind
            "fog" -> R.raw.anim_weather_fog
            "cloudy" -> R.raw.anim_weather_cloudy
            else -> R.raw.anim_not_available
        }
    }

    @Suppress("ComplexMethod")
    @RawRes
    fun getWeatherSummaryDesc(icon: String?): Int? {
        return when (icon) {
            "clear-day" -> R.string.clear
            "clear-night" -> R.string.clear
            "partly-cloudy-day" -> R.string.clear_few_low_clouds
            "partly-cloudy-night" -> R.string.clear_few_low_clouds
            "overcast-day" -> R.string.partly_cloudy
            "overcast-night" -> R.string.partly_cloudy
            "haze-day" -> R.string.clear_but_hazy
            "haze-night" -> R.string.clear_but_hazy
            "fog" -> R.string.foggy
            "extreme-day" -> R.string.mostly_cloudy
            "extreme-night" -> R.string.mostly_cloudy
            "overcast" -> R.string.overcast
            "overcast-rain" -> R.string.overcast_rain
            "overcast-snow" -> R.string.overcast_snow
            "extreme-rain" -> R.string.overcast_heavy_rain
            "extreme-snow" -> R.string.overcast_heavy_snow
            "thunderstorms-overcast-rain" -> R.string.rain_thunderstorms_likely
            "thunderstorms-light-rain" -> R.string.light_rain_thunderstorms_likely
            "thunderstorms-extreme-rain" -> R.string.heavy_rain_thunderstorms_likely
            "partly-cloudy-day-drizzle" -> R.string.mixed_with_showers
            "partly-cloudy-night-drizzle" -> R.string.mixed_with_showers
            "partly-cloudy-day-snow" -> R.string.mixed_with_snow_showers
            "partly-cloudy-night-snow" -> R.string.mixed_with_snow_showers
            "overcast-drizzle" -> R.string.overcast_light_rain
            "overcast-light-snow" -> R.string.overcast_light_snow
            "overcast-sleet" -> R.string.overcast_mixture_snow_rain
            else -> null
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
            "partly-cloudy-day-drizzle" -> R.drawable.ic_weather_partly_cloudy_day_drizzle
            "partly-cloudy-night-drizzle" -> R.drawable.ic_weather_partly_cloudy_night_drizzle
            "partly-cloudy-day-snow" -> R.drawable.ic_weather_partly_cloudy_day_snow
            "partly-cloudy-night-snow" -> R.drawable.ic_weather_partly_cloudy_night_snow
            "overcast-day" -> R.drawable.ic_weather_overcast_day
            "overcast-night" -> R.drawable.ic_weather_overcast_night
            "overcast" -> R.drawable.ic_weather_overcast
            "overcast-rain" -> R.drawable.ic_weather_overcast_rain
            "overcast-snow" -> R.drawable.ic_weather_overcast_snow
            "overcast-drizzle" -> R.drawable.ic_weather_overcast_drizzle
            "overcast-light-snow" -> R.drawable.ic_weather_overcast_light_snow
            "overcast-sleet" -> R.drawable.ic_weather_overcast_sleet
            "drizzle" -> R.drawable.ic_weather_drizzle
            "rain" -> R.drawable.ic_weather_rain
            "thunderstorms-rain" -> R.drawable.ic_weather_thunderstorms_rain
            "thunderstorms-light-rain" -> R.drawable.ic_weather_thunderstorms_rain
            "thunderstorms-overcast-rain" -> R.drawable.ic_weather_thunderstorms_overcast_rain
            "thunderstorms-extreme-rain" -> R.drawable.ic_weather_thunderstorms_extreme_rain
            "snow" -> R.drawable.ic_weather_snow
            "sleet" -> R.drawable.ic_weather_sleet
            "haze-day" -> R.drawable.ic_weather_haze_day
            "haze-night" -> R.drawable.ic_weather_haze_night
            "extreme-day" -> R.drawable.ic_weather_extreme_day
            "extreme-night" -> R.drawable.ic_weather_extreme_night
            "extreme-rain" -> R.drawable.ic_weather_extreme_rain
            "extreme-snow" -> R.drawable.ic_weather_extreme_snow
            "extreme-day-rain" -> R.drawable.ic_weather_extreme_day_rain
            "extreme-night-rain" -> R.drawable.ic_weather_extreme_night_rain
            "extreme-day-sleet" -> R.drawable.ic_weather_extreme_day_sleet
            "extreme-night-sleet" -> R.drawable.ic_weather_extreme_night_sleet
            "extreme-day-snow" -> R.drawable.ic_weather_extreme_day_snow
            "extreme-night-snow" -> R.drawable.ic_weather_extreme_night_snow
            "extreme-day-drizzle" -> R.drawable.ic_weather_extreme_day_drizzle
            "extreme-night-drizzle" -> R.drawable.ic_weather_extreme_night_drizzle
            "extreme-day-light-snow" -> R.drawable.ic_weather_extreme_day_light_snow
            "extreme-night-light-snow" -> R.drawable.ic_weather_extreme_night_light_snow
            "dust-day" -> R.drawable.ic_weather_dust_day
            "dust-night" -> R.drawable.ic_weather_dust_night
            "dust-wind" -> R.drawable.ic_weather_dust_wind
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
        includeUnit: Boolean = true
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

        val formattedValue = formatNumber(convertTemp(context, value, decimals), decimals)
        return "$formattedValue$unit"
    }

    fun getFormattedPrecipitation(
        context: Context,
        value: Float?,
        isRainRate: Boolean = true,
        includeUnit: Boolean = true
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

        val formattedValue = formatNumber(
            convertPrecipitation(context, value),
            getDecimalsPrecipitation(weatherUnit.type)
        )
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
    fun getUVClassification(context: Context, value: Int): String {
        return when {
            value <= 2 -> context.getString(R.string.uv_low)
            value <= 5 -> context.getString(R.string.uv_moderate)
            value <= 7 -> context.getString(R.string.uv_high)
            value <= 10 -> context.getString(R.string.uv_very_high)
            else -> context.getString(R.string.uv_extreme)
        }
    }

    fun getFormattedPressure(
        context: Context,
        value: Float?,
        includeUnit: Boolean = true
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

        val formattedValue =
            formatNumber(convertPressure(context, value), getDecimalsPressure(weatherUnit.type))
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
        value: Float,
        includeUnit: Boolean = true
    ): String {
        val weatherUnit = UnitSelector.getWindUnit(context)
        val unit = if (includeUnit) {
            weatherUnit.unit
        } else {
            String.empty()
        }

        val formattedValue =
            formatNumber(convertWindSpeed(context, value), getWindDecimals(weatherUnit.type))
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
        includeUnits: Boolean = true
    ): String {
        return if (windSpeed != null && windDirection != null) {
            val formattedWindSpeed = getFormattedWindSpeed(
                context = context,
                value = windSpeed,
                includeUnit = includeUnits
            )
            if (includeUnits) {
                "$formattedWindSpeed ${getFormattedWindDirection(context, windDirection)}"
            } else {
                formattedWindSpeed
            }
        } else {
            val windUnit = UnitSelector.getWindUnit(context).unit
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

    fun convertPrecipitation(context: Context, value: Number): Number {
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

    fun convertWindSpeed(context: Context, value: Number): Number {
        // Return the value based on the weather unit the user wants
        return when (UnitSelector.getWindUnit(context).type) {
            WeatherUnitType.MS -> roundToDecimals(value)
            WeatherUnitType.KMH -> roundToDecimals(UnitConverter.msToKmh(value.toFloat()))
            WeatherUnitType.MPH -> roundToDecimals(UnitConverter.msToMph(value.toFloat()))
            WeatherUnitType.KNOTS -> roundToDecimals(UnitConverter.msToKnots(value.toFloat()))
            WeatherUnitType.BEAUFORT -> UnitConverter.msToBeaufort(value.toFloat())
            else -> roundToDecimals(value)
        }
    }

    fun convertPressure(context: Context, value: Number): Number {
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

    fun getWindDecimals(unitType: WeatherUnitType): Int {
        return if (unitType == WeatherUnitType.BEAUFORT) {
            DECIMALS_WIND_BEAUFORT
        } else {
            DECIMALS_WIND_DEFAULT
        }
    }

}
