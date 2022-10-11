package com.weatherxm.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.weatherxm.ui.common.UIDevice
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
import java.time.ZonedDateTime

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
) : Parcelable {
    fun hasWallet() = wallet?.address?.isNotEmpty() == true
}

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
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class PublicDevice(
    val id: String,
    val name: String,
    val timezone: String?,
    val isActive: Boolean?,
    val lastWeatherStationActivity: ZonedDateTime?,
    val cellIndex: String?,
    @Json(name = "current_weather")
    val currentWeather: HourlyWeather?,
) : Parcelable {
    fun toUIDevice(): UIDevice {
        return UIDevice(
            id,
            name,
            cellIndex,
            isActive,
            lastWeatherStationActivity,
            timezone,
            null,
            currentWeather,
            null
        )
    }
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
    var address: String?,
    val rewards: Rewards?
) : Parcelable {
    companion object {
        fun empty() = Device(
            "", "", null, null, null, null, null, null, null
        )
    }

    fun getNameOrLabel(): String {
        return attributes?.friendlyName ?: name
    }

    fun isEmpty() = id == "" && name == ""
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Attributes(
    val isActive: Boolean?,
    val lastWeatherStationActivity: ZonedDateTime?,
    val friendlyName: String?,
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
data class Rewards(
    @Json(name = "total_rewards")
    val totalRewards: Float?,
    @Json(name = "actual_reward")
    val actualReward: Float?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class TransactionsResponse(
    val data: List<Transaction>,
    @Json(name = "total_pages")
    val totalPages: Int,
    @Json(name = "has_next_page")
    val hasNextPage: Boolean
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Transaction(
    val timestamp: ZonedDateTime,
    @Json(name = "tx_hash")
    val txHash: String?,
    @Json(name = "validation_score")
    val validationScore: Float?,
    @Json(name = "daily_reward")
    val dailyReward: Float?,
    @Json(name = "actual_reward")
    val actualReward: Float?,
    @Json(name = "total_rewards")
    val totalRewards: Float?,
) : Parcelable {
    companion object {
        /*
        * Have this very small number to use when a day is null or zero,
        * in order to have a bar in the chart in the token card view
         */
        const val VERY_SMALL_NUMBER_FOR_CHART = 0.001F
    }
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class WeatherData(
    var date: LocalDate,
    val tz: String?,
    val hourly: List<HourlyWeather>?,
    val daily: DailyData?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class HourlyWeather(
    var timestamp: ZonedDateTime,
    val precipitation: Float?,
    val temperature: Float?,
    @Json(name = "feels_like")
    val feelsLike: Float?,
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
data class PublicHex(
    val index: String,
    @Json(name = "device_count")
    val deviceCount: Int?,
    val center: Location,
    val polygon: List<Location>,
) : Parcelable

