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
            Hourly.PrecipProbability,
            Hourly.PrecipType,
            Hourly.UV
        ]
    )
    annotation class Hourly {
        companion object {
            const val Temperature = "hourly_temperature"
            const val Precipitation = "hourly_precip_intensity"
            const val Humidity = "humidity"
            const val WindSpeed = "wind_speed"
            const val WindGust = "wind_gust"
            const val WindDirection = "wind_direction"
            const val Pressure = "pressure"
            const val Cloud = "cloud_cover"
            const val UV = "uv_index"
            const val Icon = "icon"
            const val PrecipType = "precip_type"
            const val PrecipProbability = "precip_probability"
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
            Hourly.PrecipProbability,
            Hourly.PrecipType,
            Hourly.UV
        )
    }
}
