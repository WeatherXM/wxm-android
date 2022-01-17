package com.weatherxm.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.weatherxm.ui.userdevice.TokenData
import com.weatherxm.ui.userdevice.TokenSummary
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
    val timezone: String?,
    val attributes: Attributes?,
    @Json(name = "current_weather")
    val currentWeather: HourlyWeather?,
    val address: String?
) : Parcelable {
    fun getNameOrLabel(): String {
        return if (!label.isNullOrEmpty()) {
            "$label ($name)"
        } else {
            name
        }
    }
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Attributes(
    val isActive: Boolean?,
    val lastActiveAt: String?,
    val hex3: Hex,
    val hex7: Hex,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Hex(
    val index: String,
    val polygon: Array<Location>,
    val center: Location
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Tokens(
    @Json(name = "24h")
    val token24hour: TokensSummaryResponse,
    @Json(name = "7d")
    val token7days: TokensSummaryResponse,
    @Json(name = "30d")
    val token30days: TokensSummaryResponse
) : Parcelable {
    fun toTokenData(): TokenData {
        val tokenData = TokenData(
            TokenSummary(0F, mutableListOf()),
            TokenSummary(0F, mutableListOf()),
            TokenSummary(0F, mutableListOf())
        )

        tokenData.tokens24h = token24hour.toTokenSummary()
        tokenData.tokens7d = token7days.toTokenSummary()
        tokenData.tokens30d = token30days.toTokenSummary()

        return tokenData
    }
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class TokensSummaryResponse(
    val total: Float?,
    val values: List<TokenEntry>
) : Parcelable {
    fun toTokenSummary(): TokenSummary {
        val summary = TokenSummary(0F, mutableListOf())

        total?.let {
            summary.total = it
        }

        values.forEach {
            if (it.timestamp != null && it.value != null) {
                summary.values.add(Pair(it.timestamp, it.value))
            }
        }
        return summary
    }
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class TokenEntry(
    val timestamp: String?,
    val value: Float?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class WeatherData(
    var date: String?,
    val tz: String?,
    val hourly: List<HourlyWeather>?,
    val daily: DailyData?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class HourlyWeather(
    var timestamp: String?,
    val precipitation: Float?,
    val temperature: Float?,
    @Json(name = "wind_direction")
    val windDirection: Int?,
    val humidity: Int?,
    @Json(name = "wind_speed")
    val windSpeed: Float?,
    @Json(name = "wind_gust")
    val windGust: Float?,
    val icon: String?,
    @Json(name = "precipitation_probability")
    val precipProbability: Int?,
    @Json(name = "uv_index")
    val uvIndex: Int?,
    @Json(name = "cloud_cover")
    val cloudCover: Int?,
    val pressure: Float?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class DailyData(
    var timestamp: String?,
    @Json(name = "precip_intensity")
    val precipIntensity: Float?,
    @Json(name = "precipitation_type")
    val precipType: String?,
    @Json(name = "temperature_min")
    val temperatureMin: Float?,
    @Json(name = "temperature_max")
    val temperatureMax: Float?,
    @Json(name = "wind_direction")
    val windDirection: Int?,
    val humidity: Int?,
    @Json(name = "wind_speed")
    val windSpeed: Float?,
    @Json(name = "wind_gust")
    val windGust: Float?,
    @Json(name = "icon")
    val icon: String?,
    @Json(name = "precip_probability")
    val precipProbability: Int?,
    @Json(name = "uv_index")
    val uvIndex: Int?,
    @Json(name = "cloud_cover")
    val cloudCover: Int?,
    val pressure: Float?
) : Parcelable
