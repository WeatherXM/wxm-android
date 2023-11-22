package com.weatherxm.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.weatherxm.ui.common.AnnotationCode
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import kotlinx.parcelize.Parcelize
import timber.log.Timber
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
    var lat: Double,
    var lon: Double
) : Parcelable {
    companion object {
        fun empty() = Location(0.0, 0.0)
    }

    fun isEmpty(): Boolean = this.lat == 0.0 && this.lon == 0.0
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class PublicDevice(
    val id: String,
    val name: String,
    val timezone: String?,
    val profile: DeviceProfile?,
    val isActive: Boolean?,
    val lastWeatherStationActivity: ZonedDateTime?,
    val cellIndex: String,
    @Json(name = "current_weather")
    val currentWeather: HourlyWeather?,
) : Parcelable {
    fun toUIDevice(): UIDevice {
        return UIDevice(
            id = id,
            name = name,
            cellIndex = cellIndex,
            profile = profile,
            isActive = isActive,
            lastWeatherStationActivity = lastWeatherStationActivity,
            timezone = timezone,
            currentWeather = currentWeather,
            relation = null,
            label = null,
            friendlyName = null,
            location = null,
            currentFirmware = null,
            cellCenter = null,
            assignedFirmware = null,
            claimedAt = null,
            address = null
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
    val profile: DeviceProfile?,
    val attributes: Attributes?,
    @Json(name = "current_weather")
    val currentWeather: HourlyWeather?,
    var address: String?,
    val rewards: Rewards?,
    val relation: Relation?
) : Parcelable {
    companion object {
        fun empty() = Device(
            "", "", null, null, null, null, null, null, null, null, null
        )
    }

    fun toUIDevice(): UIDevice {
        val deviceRelation = when (relation) {
            Relation.followed -> DeviceRelation.FOLLOWED
            Relation.owned -> DeviceRelation.OWNED
            else -> DeviceRelation.UNFOLLOWED
        }
        return UIDevice(
            id = id,
            name = name,
            cellIndex = attributes?.hex7?.index ?: "",
            cellCenter = attributes?.hex7?.center,
            profile = profile,
            isActive = attributes?.isActive,
            lastWeatherStationActivity = attributes?.lastWeatherStationActivity,
            timezone = timezone,
            relation = deviceRelation,
            label = label,
            friendlyName = attributes?.friendlyName,
            location = location,
            currentFirmware = attributes?.firmware?.current,
            assignedFirmware = attributes?.firmware?.assigned,
            claimedAt = attributes?.claimedAt,
            address = address,
            currentWeather = currentWeather
        )
    }

    fun isEmpty() = id == "" && name == ""
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Attributes(
    val isActive: Boolean?,
    val firmware: Firmware?,
    val lastWeatherStationActivity: ZonedDateTime?,
    val claimedAt: ZonedDateTime?,
    val friendlyName: String?,
    val hex3: Hex,
    val hex7: Hex,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Firmware(
    val current: String?,
    val assigned: String?,
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
    @Json(name = "reward_score")
    val rewardScore: Int?,
    @Json(name = "daily_reward")
    val dailyReward: Float?,
    @Json(name = "actual_reward")
    val actualReward: Float?,
    @Json(name = "total_rewards")
    val totalRewards: Float?,
    @Json(name = "lost_rewards")
    val lostRewards: Float?,
    val timeline: RewardsTimeline?,
    val annotations: RewardsAnnotations?
) : Parcelable

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
    @Json(name = "precipitation_accumulated")
    val precipAccumulated: Float?,
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
    val pressure: Float?,
    @Json(name = "dew_point")
    val dewPoint: Float?,
    @Json(name = "solar_irradiance")
    val solarIrradiance: Float?
) : Parcelable {
    fun isEmpty(): Boolean {
        return precipitation == null && temperature == null && feelsLike == null
            && windDirection == null && humidity == null && windSpeed == null && windGust == null
            && icon.isNullOrEmpty() && precipProbability == null && uvIndex == null
            && cloudCover == null && pressure == null && dewPoint == null && solarIrradiance == null
    }
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class DailyData(
    var timestamp: String?,
    @Json(name = "precipitation_intensity")
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

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class DeviceInfo(
    val name: String,
    @Json(name = "claimed_at")
    val claimedAt: ZonedDateTime?,
    val gateway: Gateway?,
    @Json(name = "weather_station")
    val weatherStation: WeatherStation?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Gateway(
    val model: String?,
    val firmware: Firmware?,
    @Json(name = "last_activity")
    val lastActivity: ZonedDateTime?,
    @Json(name = "serial_number")
    val serialNumber: String?,
    @Json(name = "gps_sats")
    val gpsSats: String?,
    @Json(name = "wifi_rssi")
    val wifiRssi: String?,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class WeatherStation(
    val model: String?,
    val firmware: Firmware?,
    @Json(name = "last_activity")
    val lastActivity: ZonedDateTime?,
    @Json(name = "dev_eui")
    val devEUI: String?,
    @Json(name = "hw_version")
    val hwVersion: String?,
    @Json(name = "last_hs_name")
    val lastHotspot: String?,
    @Json(name = "last_tx_rssi")
    val lastTxRssi: String?,
    @Json(name = "bat_state")
    val batteryState: BatteryState?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NetworkStatsResponse(
    @Json(name = "weather_stations")
    val weatherStations: NetworkStatsWeatherStations,
    @Json(name = "data_days")
    val dataDays: List<NetworkStatsTimeseries>?,
    val tokens: NetworkStatsTokens?,
    val customers: NetworkStatsCustomers?,
    @Json(name = "last_updated")
    val lastUpdated: ZonedDateTime?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NetworkStatsWeatherStations(
    val onboarded: NetworkStatsStation?,
    val claimed: NetworkStatsStation?,
    val active: NetworkStatsStation?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NetworkStatsStation(
    val total: Int?,
    val details: List<NetworkStatsStationDetails>?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NetworkStatsStationDetails(
    val model: String?,
    val connectivity: Connectivity?,
    val url: String?,
    val amount: Int?,
    val percentage: Double?,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NetworkStatsTokens(
    @Json(name = "total_supply")
    val totalSupply: Int?,
    @Json(name = "daily_minted")
    val dailyMinted: Int?,
    @Json(name = "allocated_per_day")
    val allocatedPerDay: List<NetworkStatsTimeseries>?,
    @Json(name = "avg_monthly")
    val avgMonthly: Double?,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NetworkStatsCustomers(
    val total: Int?,
    @Json(name = "with_wallet")
    val withWallet: Int?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NetworkStatsTimeseries(
    val ts: ZonedDateTime?,
    val value: Double?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NetworkSearchResults(
    val devices: List<NetworkSearchDeviceResult>?,
    val addresses: List<NetworkSearchAddressResult>?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NetworkSearchDeviceResult(
    val id: String?,
    val name: String?,
    val connectivity: Connectivity?,
    @Json(name = "cell_index")
    val cellIndex: String?,
    @Json(name = "cell_center")
    val cellCenter: Location?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NetworkSearchAddressResult(
    val name: String?,
    val place: String?,
    val center: Location?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Rewards(
    @Json(name = "total_rewards")
    val totalRewards: Float?,
    val latest: RewardsObject?,
    val weekly: RewardsObject?,
    val monthly: RewardsObject?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class RewardsObject(
    val timestamp: ZonedDateTime?,
    @Json(name = "from_date")
    val fromDate: ZonedDateTime?,
    @Json(name = "to_date")
    val toDate: ZonedDateTime?,
    @Json(name = "tx_hash")
    val txHash: String?,
    @Json(name = "reward_score")
    val rewardScore: Int?,
    @Json(name = "period_max_reward")
    val periodMaxReward: Float?,
    @Json(name = "actual_reward")
    val actualReward: Float?,
    @Json(name = "lost_rewards")
    val lostRewards: Float?,
    val timeline: RewardsTimeline?,
    val annotations: RewardsAnnotations?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class RewardsTimeline(
    @Json(name = "reference_date")
    val referenceDate: ZonedDateTime?,
    @Json(name = "reward_scores")
    val rewardScores: List<Int>?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class RewardsAnnotations(
    val qod: List<RewardsAnnotation>?,
    val pol: List<RewardsAnnotation>?,
    val rm: List<RewardsAnnotation>?,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class RewardsAnnotation(
    val annotation: String?,
    val ratio: Int?,
    val affects: List<QoDErrorAffects>?,
) : Parcelable {
    fun toAnnotationCode(): AnnotationCode {
        return try {
            AnnotationCode.valueOf(annotation ?: "")
        } catch (e: IllegalArgumentException) {
            Timber.w(e)
            AnnotationCode.UNKNOWN
        }
    }
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class QoDErrorAffects(
    val parameter: String?,
    val ratio: Int?,
) : Parcelable

enum class DeviceProfile {
    M5,
    Helium
}

@Suppress("EnumNaming")
enum class Connectivity {
    wifi,
    helium,
    cellular
}

@Suppress("EnumNaming")
enum class BatteryState {
    low,
    ok
}

@Suppress("EnumNaming")
enum class Relation {
    owned,
    followed
}

