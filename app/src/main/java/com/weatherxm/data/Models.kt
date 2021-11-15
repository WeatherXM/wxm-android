package com.weatherxm.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class User(
    val id: String,
    val email: String,
    val name: String?,
    val firstName: String?,
    val lastName: String?,
    val wallet: Wallet?,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Wallet(
    val address: String?,
    val updatedAt: Long?,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Location(
    val lat: Double,
    val lon: Double
) : Parcelable {
    companion object {
        fun empty() = Location(0.0, 0.0)
    }

    fun isEmpty() = this.lat == 0.0 && this.lon == 0.0
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Device(
    val id: String,
    val name: String,
    val label: String?,
    val location: Location?,
    val attributes: Attributes?,
    val timeseries: Timeseries?,
    val address: String?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Attributes(
    val isActive: Boolean?,
    val lastActiveAt: Long?,
    val hex3: Hex,
    val hex7: Hex,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Timeseries(
    val timestamp: Long?,
    @Json(name = "hourly_precip_intensity")
    val hourlyPrecipIntensity: Float?,
    @Json(name = "hourly_temperature")
    val hourlyTemperature: Float?,
    @Json(name = "hourly_wind_direction")
    val hourlyWindDirection: Int?,
    @Json(name = "hourly_humidity")
    val hourlyHumidity: Int?,
    @Json(name = "hourly_wind_speed")
    val hourlyWindSpeed: Float?,
    @Json(name = "hourly_icon")
    val hourlyIcon: String?,
    @Json(name = "hourly_dew_point")
    val hourlyDewPoint: Float?,
    @Json(name = "hourly_precip_probability")
    val hourlyPrecipProbability: Int?,
    @Json(name = "hourly_uv_index")
    val hourlyUvIndex: Int?,
    @Json(name = "hourly_pressure")
    val hourlyPressure: Float?,
    @Json(name = "hourly_cloud_cover")
    val hourlyCloudCover: Int?,
    @Json(name = "hourly_wind_gust")
    val hourlyWindGust: Float?,
    @Json(name = "hourly_precip_type")
    val hourlyPrecipType: String?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Hex(
    val index: String,
    val polygon: Array<Location>,
    val center: Location
) : Parcelable
