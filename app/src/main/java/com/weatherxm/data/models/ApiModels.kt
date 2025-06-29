package com.weatherxm.data.models

import android.os.Parcelable
import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.weatherxm.ui.common.AnnotationGroupCode
import com.weatherxm.ui.common.BundleName
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.DeviceTotalRewardsBoost
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.util.Rewards.THRESHOLD_SCORE_TO_SHOW_ISSUES
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.roundToInt

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
data class Bundle(
    val name: String?,
    val title: String?,
    val connectivity: String?,
    @Json(name = "ws_model")
    val wsModel: String?,
    @Json(name = "gw_model")
    val gwModel: String?,
    @Json(name = "hw_class")
    val hwClass: String?,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class PublicDevice(
    val id: String,
    val name: String,
    val timezone: String?,
    val bundle: Bundle?,
    val isActive: Boolean?,
    val lastWeatherStationActivity: ZonedDateTime?,
    val cellIndex: String,
    val cellCenter: Location?,
    val address: String?,
    val metrics: Metrics?,
    val cellAvgDataQuality: Int?,
    @Json(name = "current_weather")
    val currentWeather: HourlyWeather?,
) : Parcelable {
    fun toUIDevice(): UIDevice {
        return UIDevice(
            id = id,
            name = name,
            cellDataQuality = cellAvgDataQuality,
            cellIndex = cellIndex,
            cellCenter = cellCenter,
            bundleName = try {
                BundleName.valueOf(bundle?.name ?: String.empty())
            } catch (e: IllegalArgumentException) {
                Timber.e("Wrong Bundle Name: ${bundle?.name} for Device $name")
                null
            },
            bundleTitle = bundle?.title,
            connectivity = bundle?.connectivity,
            wsModel = bundle?.wsModel,
            gwModel = bundle?.gwModel,
            hwClass = bundle?.hwClass,
            isActive = isActive,
            lastWeatherStationActivity = lastWeatherStationActivity,
            timezone = timezone,
            currentWeather = currentWeather,
            address = address,
            qodScore = metrics?.qodScore,
            polReason = metrics?.polToAnnotationGroupCode(),
            metricsTimestamp = metrics?.ts,
            relation = null,
            label = null,
            friendlyName = null,
            location = null,
            currentFirmware = null,
            assignedFirmware = null,
            claimedAt = null,
            hex7 = null,
            totalRewards = null,
            actualReward = null,
            hasLowBattery = null
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
    val bundle: Bundle?,
    val attributes: Attributes?,
    @Json(name = "current_weather")
    val currentWeather: HourlyWeather?,
    var address: String?,
    val rewards: Rewards?,
    val relation: Relation?,
    val metrics: Metrics?,
    @Json(name = "bat_state")
    val batteryState: BatteryState?
) : Parcelable {
    companion object {
        fun empty() = Device(
            String.empty(),
            String.empty(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
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
            cellDataQuality = null,
            cellIndex = attributes?.hex7?.index ?: String.empty(),
            cellCenter = attributes?.hex7?.center,
            bundleName = try {
                BundleName.valueOf(bundle?.name ?: String.empty())
            } catch (e: IllegalArgumentException) {
                Timber.e("Wrong Bundle Name: ${bundle?.name} for Device $name")
                null
            },
            bundleTitle = bundle?.title,
            connectivity = bundle?.connectivity,
            wsModel = bundle?.wsModel,
            gwModel = bundle?.gwModel,
            hwClass = bundle?.hwClass,
            isActive = attributes?.isActive,
            lastWeatherStationActivity = attributes?.lastWeatherStationActivity,
            timezone = timezone,
            relation = deviceRelation,
            label = label,
            friendlyName = attributes?.friendlyName,
            location = location,
            hex7 = attributes?.hex7,
            currentFirmware = attributes?.firmware?.current,
            assignedFirmware = attributes?.firmware?.assigned,
            claimedAt = attributes?.claimedAt,
            address = address,
            currentWeather = currentWeather,
            totalRewards = rewards?.totalRewards,
            actualReward = rewards?.actualReward,
            qodScore = metrics?.qodScore,
            polReason = metrics?.polToAnnotationGroupCode(),
            metricsTimestamp = metrics?.ts,
            hasLowBattery = batteryState == BatteryState.low
        )
    }

    fun isEmpty() = id == String.empty() && name == String.empty()
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Metrics(
    @Json(name = "qod_score")
    val qodScore: Int?,
    @Json(name = "pol_reason")
    val polReason: String?,
    val ts: ZonedDateTime?
) : Parcelable {
    fun polToAnnotationGroupCode(): AnnotationGroupCode? {
        return if (polReason.isNullOrEmpty()) {
            null
        } else {
            try {
                AnnotationGroupCode.valueOf(polReason)
            } catch (e: IllegalArgumentException) {
                Timber.w(e)
                AnnotationGroupCode.UNKNOWN
            }
        }
    }
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
    val hex3: Hex?,
    val hex7: Hex?,
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
data class RewardsTimeline(
    val data: List<Reward>,
    @Json(name = "total_pages")
    val totalPages: Int,
    @Json(name = "has_next_page")
    val hasNextPage: Boolean
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
    @Json(name = "active_device_count")
    val activeDeviceCount: Int?,
    @Json(name = "avg_data_quality")
    val avgDataQuality: Int?,
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
    @Json(name = "reward_split")
    val rewardSplit: List<RewardSplit>?,
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
    @Json(name = "gps_sats_last_activity")
    val gpsSatsLastActivity: ZonedDateTime?,
    @Json(name = "wifi_rssi")
    val wifiRssi: String?,
    @Json(name = "wifi_rssi_last_activity")
    val wifiRssiLastActivity: ZonedDateTime?,
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
    @Json(name = "last_hs_name_last_activity")
    val lastHotspotLastActivity: ZonedDateTime?,
    @Json(name = "last_tx_rssi")
    val lastTxRssi: String?,
    @Json(name = "last_tx_rssi_last_activity")
    val lastTxRssiLastActivity: ZonedDateTime?,
    @Json(name = "station_rssi")
    val stationRssi: Int?,
    @Json(name = "station_rssi_last_activity")
    val stationRssiLastActivity: ZonedDateTime?,
    @Json(name = "bat_state")
    val batteryState: BatteryState?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NetworkStatsResponse(
    @Json(name = "net_health")
    val health: NetworkStatsHealth?,
    @Json(name = "net_growth")
    val growth: NetworkStatsGrowth?,
    val rewards: NetworkStatsRewards?,
    @Json(name = "weather_stations")
    val weatherStations: NetworkStatsWeatherStations,
    val contracts: NetworkStatsContracts?,
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
data class NetworkStatsHealth(
    @Json(name = "network_avg_qod")
    val networkAvgQod: Int?,
    @Json(name = "active_stations")
    val activeStations: Int?,
    @Json(name = "network_uptime")
    val networkUptime: Int?,
    @Json(name = "health_30days_graph")
    val health30DaysGraph: List<NetworkStatsTimeseries>?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NetworkStatsGrowth(
    @Json(name = "network_size")
    val networkSize: Int?,
    @Json(name = "network_scale_up")
    val networkScaleUp: Int?,
    @Json(name = "last_30days")
    val last30Days: Int?,
    @Json(name = "last_30days_graph")
    val last30DaysGraph: List<NetworkStatsTimeseries>?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NetworkStatsRewards(
    val total: Int?,
    @Json(name = "last_run")
    val lastRun: Int?,
    @Json(name = "last_30days")
    val last30Days: Int?,
    @Json(name = "last_30days_graph")
    val last30DaysGraph: List<NetworkStatsTimeseries>?,
    @Json(name = "last_tx_hash_url")
    val lastTxHashUrl: String?,
    @Json(name = "token_metrics")
    val tokenMetrics: NetworkStatsTokenMetrics?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NetworkStatsTokenMetrics(
    @Json(name = "total_allocated")
    val totalAllocated: NetworkStatsTotalAllocated?,
    val token: NetworkStatsToken?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NetworkStatsTotalAllocated(
    val dune: NetworkStatsDune?,
    @Json(name = "base_rewards")
    val baseRewards: Int?,
    @Json(name = "boost_rewards")
    val boostRewards: Int?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NetworkStatsDune(
    val total: Int?,
    val claimed: Int?,
    val unclaimed: Int?,
    @Json(name = "dune_public_url")
    val duneUrl: String?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NetworkStatsToken(
    @Json(name = "total_supply")
    val totalSupply: Int?,
    @Json(name = "circulating_supply")
    val circulatingSupply: Int?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NetworkStatsContracts(
    @Json(name = "token_url")
    val tokenUrl: String?,
    @Json(name = "rewards_url")
    val rewardsUrl: String?,
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
    val bundle: Bundle?,
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
    @Json(name = "actual_reward")
    val actualReward: Float?,
    val latest: Reward?,
    val timeline: List<RewardsTimestampScore>?
) : Parcelable {
    fun isEmpty() = latest == null && timeline == null
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Reward(
    val timestamp: ZonedDateTime?,
    @Json(name = "base_reward")
    val baseReward: Float?,
    @Json(name = "total_business_boost_reward")
    val totalBoostReward: Float?,
    @Json(name = "total_reward")
    val totalReward: Float?,
    @Json(name = "base_reward_score")
    val baseRewardScore: Int?,
    @Json(name = "annotation_summary")
    val annotationSummary: List<RewardsAnnotationGroup>?,
) : Parcelable {
    companion object {
        fun initWithTimestamp(ts: ZonedDateTime?) = Reward(ts, null, null, null, null, null)
    }

    fun shouldShowAnnotations() = !annotationSummary.isNullOrEmpty()
        && baseRewardScore != null
        && baseRewardScore < THRESHOLD_SCORE_TO_SHOW_ISSUES
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class RewardsTimestampScore(
    val timestamp: ZonedDateTime?,
    @Json(name = "base_reward_score")
    val baseRewardScore: Int?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class RewardsAnnotationGroup(
    @Json(name = "severity_level")
    val severityLevel: SeverityLevel?,
    val group: String?,
    val title: String?,
    val message: String?,
    @Json(name = "doc_url")
    val docUrl: String?,
) : Parcelable {
    fun toAnnotationGroupCode(): AnnotationGroupCode {
        return try {
            AnnotationGroupCode.valueOf(group ?: String.empty())
        } catch (e: IllegalArgumentException) {
            Timber.w(e)
            AnnotationGroupCode.UNKNOWN
        }
    }

    fun isInfo() = severityLevel == SeverityLevel.INFO
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class RewardDetails(
    val timestamp: ZonedDateTime?,
    @Json(name = "total_daily_reward")
    val totalDailyReward: Float?,
    val base: BaseReward?,
    val boost: BoostRewards?,
    @Json(name = "annotation_summary")
    val annotationSummary: List<RewardsAnnotationGroup>?,
    @Json(name = "reward_split")
    val rewardSplit: List<RewardSplit>?
) : Parcelable {
    companion object {
        fun empty() = RewardDetails(null, null, null, null, null, null)
    }

    fun toSortedAnnotations(): List<RewardsAnnotationGroup>? {
        return annotationSummary?.sortedByDescending {
            it.severityLevel
        }
    }

    fun hasSplitRewards() = !rewardSplit.isNullOrEmpty() && rewardSplit.size >= 2

    fun isEmpty() = timestamp == null && totalDailyReward == null && base == null
        && boost == null && annotationSummary == null && rewardSplit == null

    fun shouldShowAnnotations() = !annotationSummary.isNullOrEmpty()
        && base?.rewardScore != null
        && base.rewardScore < THRESHOLD_SCORE_TO_SHOW_ISSUES
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class BaseReward(
    @Json(name = "actual_reward")
    val actualReward: Float?,
    @Json(name = "reward_score")
    val rewardScore: Int?,
    @Json(name = "max_reward")
    val maxReward: Float?,
    @Json(name = "qod_score")
    val qodScore: Int?,
    @Json(name = "cell_capacity")
    val cellCapacity: Int?,
    @Json(name = "cell_position")
    val cellPosition: Int?,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class BoostRewards(
    @Json(name = "total_daily_reward")
    val totalDailyReward: Float?,
    val data: List<BoostReward>
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class BoostReward(
    val code: String?,
    val title: String?,
    val description: String?,
    @Json(name = "img_url")
    val imgUrl: String?,
    @Json(name = "doc_url")
    val docUrl: String?,
    @Json(name = "actual_reward")
    val actualReward: Float?,
    @Json(name = "reward_score")
    val rewardScore: Int?,
    @Json(name = "max_reward")
    val maxReward: Float?,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class BoostRewardResponse(
    val code: String?,
    val metadata: BoostRewardMetadata?,
    val details: BoostRewardDetails?,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class BoostRewardMetadata(
    val title: String?,
    val description: String?,
    @Json(name = "img_url")
    val imgUrl: String?,
    @Json(name = "doc_url")
    val docUrl: String?,
    val about: String?,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class BoostRewardDetails(
    @Json(name = "station_hours")
    val stationHours: Int?,
    @Json(name = "max_daily_reward")
    val maxDailyReward: Float?,
    @Json(name = "max_total_reward")
    val maxTotalReward: Float?,
    @Json(name = "boost_start_date")
    val boostStartDate: ZonedDateTime?,
    @Json(name = "boost_stop_date")
    val boostStopDate: ZonedDateTime?,
    @Json(name = "participation_start_date")
    val participationStartDate: ZonedDateTime?,
    @Json(name = "participation_stop_date")
    val participationStopDate: ZonedDateTime?,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class QoDErrorAffects(
    val parameter: String?,
    val ratio: Int?,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class WalletRewards(
    val proof: List<String>?,
    @Json(name = "cumulative_amount")
    val cumulativeAmount: Double?,
    val cycle: Int?,
    val available: Double?,
    @Json(name = "total_claimed")
    val totalClaimed: Double?,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class RewardSplit(
    val wallet: String,
    val stake: Float,
    val reward: Float?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class DevicesRewards(
    val total: Float?,
    val data: List<DevicesRewardsData>?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class DevicesRewardsData(
    val ts: ZonedDateTime,
    @Json(name = "total_rewards")
    val totalRewards: Float?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class DeviceRewardsSummary(
    val total: Float?,
    val data: List<DeviceRewardsSummaryData>?,
    val details: List<DeviceRewardsSummaryDetails>?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class DeviceRewardsSummaryData(
    val ts: ZonedDateTime,
    val rewards: List<DeviceRewardsSummaryDataReward>?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class DeviceRewardsSummaryDataReward(
    val type: String,
    val code: String,
    val value: Float,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class DeviceRewardsSummaryDetails(
    val type: String?,
    val code: String?,
    @Json(name = "current_rewards")
    val currentRewards: Float?,
    @Json(name = "total_rewards")
    val totalRewards: Float?,
    @Json(name = "boost_period_start")
    val boostPeriodStart: ZonedDateTime?,
    @Json(name = "boost_period_end")
    val boostPeriodEnd: ZonedDateTime?,
    @Json(name = "completed_percentage")
    val completedPercentage: Float?
) : Parcelable {
    fun toDeviceTotalRewardsBoost(): DeviceTotalRewardsBoost {
        return DeviceTotalRewardsBoost(
            code,
            completedPercentage?.roundToInt(),
            totalRewards,
            currentRewards,
            boostPeriodStart,
            boostPeriodEnd
        )
    }
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class PhotoPresignedMetadata(
    val url: String,
    val fields: AWSMetadata
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class AWSMetadata(
    val bucket: String?,
    @Json(name = "X-Amz-Algorithm")
    val xAmzAlgo: String?,
    @Json(name = "X-Amz-Credential")
    val xAmzCredentials: String?,
    @Json(name = "X-Amz-Date")
    val xAmzDate: String?,
    @Json(name = "X-Amz-Security-Token")
    val xAmzSecurityToken: String?,
    @Json(name = "X-Amz-Signature")
    val xAmzSignature: String?,
    val key: String?,
    @Json(name = "Policy")
    val policy: String?,
) : Parcelable

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

enum class SeverityLevel {
    INFO,
    WARNING,
    ERROR
}

@Suppress("EnumNaming")
enum class RewardsCode {
    base_reward,
    beta_rewards
}

@Suppress("EnumNaming")
enum class BoostCode {
    beta_rewards
}

