package com.weatherxm.common

import androidx.annotation.StringDef

object Measurements {

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(
        value = [
            Hourly.Temperature,
            Hourly.Precipitation,
            Hourly.Humidity,
            Hourly.WindSpeed,
            Hourly.WindGust,
            Hourly.WindDirection,
            Hourly.Pressure,
            Hourly.Icon,
            Hourly.Cloud,
            Hourly.DewPoint,
            Hourly.PrecipProbability,
            Hourly.PrecipType,
            Hourly.UV
        ]
    )
    annotation class Hourly {
        companion object {
            const val Temperature = "hourly_temperature"
            const val Precipitation = "hourly_precip_intensity"
            const val Humidity = "hourly_humidity"
            const val WindSpeed = "hourly_wind_speed"
            const val WindGust = "hourly_wind_gust"
            const val WindDirection = "hourly_wind_direction"
            const val Pressure = "hourly_pressure"
            const val Cloud = "hourly_cloud_cover"
            const val UV = "hourly_uv_index"
            const val Icon = "hourly_icon"
            const val PrecipType = "hourly_precip_type"
            const val PrecipProbability = "hourly_precip_probability"
            const val DewPoint = "hourly_dew_point"
        }
    }

    fun getHourlyKeys(): Array<String> {
        return arrayOf(
            Hourly.Temperature,
            Hourly.Precipitation,
            Hourly.Humidity,
            Hourly.WindSpeed,
            Hourly.WindGust,
            Hourly.WindDirection,
            Hourly.Pressure,
            Hourly.Icon,
            Hourly.Cloud,
            Hourly.DewPoint,
            Hourly.PrecipProbability,
            Hourly.PrecipType,
            Hourly.UV
        )
    }
}
