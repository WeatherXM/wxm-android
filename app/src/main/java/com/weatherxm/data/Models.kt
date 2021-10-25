package com.weatherxm.data

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class EntityId(
    val id: String,
    val entityType: String
) : Parcelable

@Parcelize
enum class Authority(
    val authority: String
) : Parcelable {
    SYS_ADMIN("SYS_ADMIN"),
    TENANT_ADMIN("TENANT_ADMIN"),
    CUSTOMER_USER("CUSTOMER_USER"),
    REFRESH_TOKEN("REFRESH_TOKEN"),
    ANONYMOUS("ANONYMOUS")
}

@JsonClass(generateAdapter = true)
@Parcelize
data class User(
    val id: EntityId,
    val email: String,
    val authority: Authority,
    val name: String?,
    val firstName: String?,
    val lastName: String?,
    val createdTime: Long,
    val tenantId: EntityId,
    val customerId: EntityId
) : Parcelable

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

@JsonClass(generateAdapter = true)
@Parcelize
data class Device(
    val id: EntityId,
    val createdTime: Long,
    val name: String,
    val type: String,
    val label: String?,
    val deviceProfileId: EntityId,
    val location: Location?,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class PublicDevice(
    val id: String,
    val name: String,
    val label: String?,
    val location: Location?,
    val attributes: Attributes?,
    val timeseries: Timeseries?
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class Attributes(
    val hex3: Hex,
    val hex7: Hex,
) : Parcelable

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

@JsonClass(generateAdapter = true)
@Parcelize
data class Hex(
    val index: String,
    val polygon: Array<Location>,
    val center: Location
) : Parcelable
