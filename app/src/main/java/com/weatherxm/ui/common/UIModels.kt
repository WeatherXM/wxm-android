package com.weatherxm.ui.common

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.models.Hex
import com.weatherxm.data.models.HourlyWeather
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.Reward
import com.weatherxm.data.models.RewardSplit
import com.weatherxm.data.models.SeverityLevel
import com.weatherxm.data.repository.RewardsRepositoryImpl
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
import java.time.ZonedDateTime

@Keep
data class UIError(
    var errorMessage: String,
    var errorCode: String? = null,
    var retryFunction: (() -> Unit)? = null
)

@Keep
@JsonClass(generateAdapter = true)
data class UIRewardsTimeline(
    var rewards: List<TimelineReward>,
    var hasNextPage: Boolean = false
)

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class TimelineReward(
    val type: RewardTimelineType,
    val data: Reward?
) : Parcelable

@Suppress("TooManyFunctions")
@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class UIDevice(
    val id: String,
    val name: String,
    val cellIndex: String,
    var relation: DeviceRelation?,
    val label: String?,
    var friendlyName: String?,
    val bundleName: BundleName?,
    val bundleTitle: String?,
    val connectivity: String?,
    val wsModel: String?,
    val gwModel: String?,
    val hwClass: String?,
    val location: Location?,
    var cellCenter: Location?,
    var hex7: Hex?,
    val isActive: Boolean?,
    val currentFirmware: String?,
    val assignedFirmware: String?,
    val claimedAt: ZonedDateTime?,
    val lastWeatherStationActivity: ZonedDateTime?,
    var timezone: String?,
    val address: String?,
    @Json(name = "current_weather")
    val currentWeather: HourlyWeather?,
    val hasLowBattery: Boolean?,
    val totalRewards: Float?,
    val actualReward: Float?,
    val qodScore: Int?,
    val polReason: AnnotationGroupCode?,
    val metricsTimestamp: ZonedDateTime?,
    var alerts: List<DeviceAlert> = listOf(),
    val isDeviceFromSearchResult: Boolean = false
) : Parcelable {
    companion object {
        fun empty() = UIDevice(
            String.empty(),
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
            null,
            null,
            null,
            null,
            null,
            null
        )
    }

    fun isOwned(): Boolean = relation == DeviceRelation.OWNED

    fun isUnfollowed(): Boolean = relation == DeviceRelation.UNFOLLOWED

    fun isFollowed(): Boolean = relation == DeviceRelation.FOLLOWED

    fun shouldPromptUpdate(): Boolean {
        return isOwned()
            && !currentFirmware.equals(assignedFirmware)
            && !assignedFirmware.isNullOrEmpty()
            && (bundleName == BundleName.h1 || bundleName == BundleName.h2)
    }

    fun getDefaultOrFriendlyName(): String {
        return friendlyName ?: name
    }

    /**
     * We use "6" as default because that's what is needed to match a UIDevice to a BLE scanned one
     */
    @Suppress("MagicNumber")
    fun getLastCharsOfLabel(charCount: Int = 6): String {
        val cleanLabel = label?.replace(":", String.empty())
        return cleanLabel?.substring(cleanLabel.length - charCount, cleanLabel.length)
            ?: String.empty()
    }

    fun isEmpty() = id.isEmpty() && name.isEmpty() && cellIndex.isEmpty()
    fun isOnline() = isActive != null && isActive == true

    fun hasErrors(): Boolean {
        return alerts.firstOrNull {
            it.severity == SeverityLevel.ERROR
        } != null
    }

    fun createDeviceAlerts(shouldNotifyOTA: Boolean): List<DeviceAlert> {
        val alerts = mutableListOf<DeviceAlert>()
        if (isActive == false) {
            alerts.add(DeviceAlert.createError(DeviceAlertType.OFFLINE))
        }

        if (shouldNotifyOTA && shouldPromptUpdate()) {
            alerts.add(DeviceAlert.createWarning(DeviceAlertType.NEEDS_UPDATE))
        }

        if (hasLowBattery == true && isOwned()) {
            alerts.add(DeviceAlert.createWarning(DeviceAlertType.LOW_BATTERY))
        }
        this.alerts = alerts.sortedByDescending { alert ->
            alert.severity
        }
        return alerts
    }

    fun isHelium() = connectivity == "helium"
    fun isWifi() = connectivity == "wifi"
    fun isCellular() = connectivity == "cellular"

    fun normalizedName(): String {
        return if (!isEmpty()) {
            name.replace(" ", "-").lowercase()
        } else {
            String.empty()
        }
    }
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class DeviceAlert(
    val alert: DeviceAlertType,
    val severity: SeverityLevel,
) : Parcelable {
    companion object {
        fun createWarning(alert: DeviceAlertType): DeviceAlert {
            return DeviceAlert(alert, SeverityLevel.WARNING)
        }

        fun createError(alert: DeviceAlertType): DeviceAlert {
            return DeviceAlert(alert, SeverityLevel.ERROR)
        }
    }
}

@Keep
data class ScannedDevice(
    val address: String,
    val name: String?,
    val type: DeviceType = DeviceType.HELIUM
) {
    companion object {
        fun empty() = ScannedDevice(String.empty(), String.empty())
    }
}

@Keep
data class FrequencyState(
    val country: String?,
    val frequencies: List<String>
)

@Parcelize
enum class DeviceType : Parcelable {
    M5_WIFI,
    D1_WIFI,
    PULSE_4G,
    HELIUM
}

@Parcelize
enum class DeviceRelation : Parcelable {
    OWNED,
    FOLLOWED,
    UNFOLLOWED
}

@Parcelize
enum class DeviceAlertType : Parcelable {
    OFFLINE,
    LOW_BATTERY,
    NEEDS_UPDATE,
    LOW_STATION_RSSI
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class UIForecast(
    val next24Hours: List<HourlyWeather>?,
    val forecastDays: List<UIForecastDay>
) : Parcelable {
    companion object {
        fun empty() = UIForecast(mutableListOf(), mutableListOf())
    }

    fun isEmpty(): Boolean = next24Hours.isNullOrEmpty() && forecastDays.isEmpty()
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class UIForecastDay(
    val date: LocalDate,
    val icon: String?,
    var minTemp: Float?,
    var maxTemp: Float?,
    val precipProbability: Int?,
    val precip: Float?,
    val windSpeed: Float?,
    val windDirection: Int?,
    val humidity: Int?,
    val pressure: Float?,
    val uv: Int?,
    val hourlyWeather: List<HourlyWeather>?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
data class DevicesSortFilterOptions(
    var sortOrder: DevicesSortOrder = DevicesSortOrder.DATE_ADDED,
    var filterType: DevicesFilterType = DevicesFilterType.ALL,
    var groupBy: DevicesGroupBy = DevicesGroupBy.NO_GROUPING
) {
    fun applySort(devices: List<UIDevice>): List<UIDevice> {
        return when (sortOrder) {
            DevicesSortOrder.DATE_ADDED -> devices
            DevicesSortOrder.NAME -> devices.sortedBy { it.getDefaultOrFriendlyName() }
            DevicesSortOrder.LAST_ACTIVE -> devices.sortedByDescending {
                it.lastWeatherStationActivity
            }
        }
    }

    fun applyFilter(devices: List<UIDevice>): List<UIDevice> {
        return when (filterType) {
            DevicesFilterType.ALL -> devices
            DevicesFilterType.OWNED -> devices.filter {
                it.isOwned()
            }
            DevicesFilterType.FAVORITES -> devices.filter {
                it.relation == DeviceRelation.FOLLOWED
            }
        }
    }

    fun applyGroupBy(devices: List<UIDevice>): List<UIDevice> {
        val groupedDevices = mutableListOf<UIDevice>()
        when (groupBy) {
            DevicesGroupBy.RELATIONSHIP -> devices.groupBy { it.relation }.forEach {
                if (it.key == DeviceRelation.OWNED) {
                    groupedDevices.addAll(0, it.value)
                } else {
                    groupedDevices.addAll(it.value)
                }
            }
            DevicesGroupBy.STATUS -> devices.groupBy { it.isActive }.forEach {
                groupedDevices.addAll(it.value)
            }
            else -> groupedDevices.addAll(devices)
        }
        return groupedDevices
    }

    fun getSortAnalyticsValue(): String {
        return when (sortOrder) {
            DevicesSortOrder.DATE_ADDED -> {
                AnalyticsService.ParamValue.FILTERS_SORT_DATE_ADDED.paramValue
            }
            DevicesSortOrder.NAME -> {
                AnalyticsService.ParamValue.FILTERS_SORT_NAME.paramValue
            }
            DevicesSortOrder.LAST_ACTIVE -> {
                AnalyticsService.ParamValue.FILTERS_SORT_LAST_ACTIVE.paramValue
            }
        }
    }

    fun getFilterAnalyticsValue(): String {
        return when (filterType) {
            DevicesFilterType.ALL -> AnalyticsService.ParamValue.FILTERS_FILTER_ALL.paramValue
            DevicesFilterType.OWNED -> AnalyticsService.ParamValue.FILTERS_FILTER_OWNED.paramValue
            DevicesFilterType.FAVORITES -> {
                AnalyticsService.ParamValue.FILTERS_FILTER_FAVORITES.paramValue
            }
        }
    }

    fun getGroupByAnalyticsValue(): String {
        return when (groupBy) {
            DevicesGroupBy.NO_GROUPING -> {
                AnalyticsService.ParamValue.FILTERS_GROUP_NO_GROUPING.paramValue
            }
            DevicesGroupBy.RELATIONSHIP -> {
                AnalyticsService.ParamValue.FILTERS_GROUP_RELATIONSHIP.paramValue
            }
            DevicesGroupBy.STATUS -> AnalyticsService.ParamValue.FILTERS_GROUP_STATUS.paramValue
        }
    }
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class UIWalletRewards(
    var totalEarned: Double,
    var totalClaimed: Double,
    var allocated: Double,
    var walletAddress: String
) : Parcelable {
    companion object {
        fun empty() = UIWalletRewards(0.0, 0.0, 0.0, String.empty())
    }
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class UIBoost(
    val title: String,
    val actualReward: String,
    val boostScore: Int?,
    val lostRewards: String,
    val boostDesc: String,
    val about: String,
    val docUrl: String,
    val imgUrl: String,
    val details: List<BoostDetailInfo>
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class BoostDetailInfo(
    val title: String,
    val value: String
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class RewardSplitsData(
    var splits: List<RewardSplit>,
    var wallet: String
) : Parcelable {
    fun hasSplitRewards() = splits.size >= 2
}

@Keep
@JsonClass(generateAdapter = true)
data class Charts(
    var date: LocalDate,
    var temperature: LineChartData,
    var feelsLike: LineChartData,
    var precipitation: LineChartData,
    var precipitationAccumulated: LineChartData,
    var precipProbability: LineChartData,
    var windSpeed: LineChartData,
    var windGust: LineChartData,
    var windDirection: LineChartData,
    var humidity: LineChartData,
    var pressure: LineChartData,
    var uv: LineChartData,
    var solarRadiation: LineChartData
) {
    fun isEmpty(): Boolean {
        return !temperature.isDataValid() && !feelsLike.isDataValid()
            && !precipitation.isDataValid() && !precipitationAccumulated.isDataValid()
            && !windSpeed.isDataValid() && !windGust.isDataValid()
            && !precipProbability.isDataValid() && !windDirection.isDataValid()
            && !humidity.isDataValid() && !pressure.isDataValid() && !uv.isDataValid()
            && !solarRadiation.isDataValid()
    }
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class LineChartData(
    var timestamps: MutableList<String>,
    var entries: MutableList<Entry>
) : Parcelable {
    companion object {
        fun empty() = LineChartData(mutableListOf(), mutableListOf())
    }

    fun isDataValid(): Boolean {
        return timestamps.isNotEmpty() && entries.filterNot { it.y.isNaN() }.isNotEmpty()
    }

    /**
     * In order to implement the gaps in charts we need to use multiple LineDataSets, one per gap.
     * This is the implementation here. Source:
     * https://github.com/PhilJay/MPAndroidChart/issues/1435
     */
    fun getLineDataSetsWithValues(label: String): MutableList<LineDataSet> {
        val dataSets = mutableListOf<LineDataSet>()
        var tempEntries = mutableListOf<Entry>()

        /**
         * Based on the documentation above, we need a different LineDataSet for each continuous
         * line (with no gaps) for our entries.
         */
        entries.forEach {
            /**
             * If the value is not NaN (which means that there is a value and we are NOT in a gap)
             */
            if (!it.y.isNaN()) {
                /**
                 * If the tempEntries (our "last continuous line" or last LineDataSet) is NOT empty
                 * and the last X value of its last entry is not the previous one of the one we are
                 * traversing right now, then that means that the entry we are traversing right now
                 * is the next one after a gap and we need to add the previous tempEntries as a
                 * different LineDataSet and reset the tempEntries to be able to accept the new
                 * list of continuous (with no gaps) entries
                 */
                if (tempEntries.isNotEmpty() && tempEntries.last().x != it.x - 1) {
                    dataSets.add(LineDataSet(tempEntries, label))
                    tempEntries = mutableListOf()
                }
                tempEntries.add(it)
            }
        }
        /**
         * If we reached the end of the entries list and we have a pending LineDataSet to add
         */
        if (tempEntries.isNotEmpty()) {
            dataSets.add(LineDataSet(tempEntries, label))
        }

        return dataSets
    }

    fun getEmptyLineDataSets(label: String): MutableList<LineDataSet> {
        val dataSets = mutableListOf<LineDataSet>()
        var tempEntries = mutableListOf<Entry>()

        /**
         * Based on the documentation above, we need a different LineDataSet for each continuous
         * gap for our entries.
         */
        entries.forEach {
            /**
             * If the value is NaN (which means we are in a gap)
             */
            if (it.y.isNaN()) {
                /**
                 * If the tempEntries (our "last continuous line" or last LineDataSet) is NOT empty
                 * and the last X value of its last entry is not the previous one of the one we are
                 * traversing right now, then that means that the entry we are traversing right now
                 * is the first one in a gap and we need to add the previous tempEntries as a
                 * different LineDataSet and reset the tempEntries to be able to accept the new
                 * list of continuous gap entries
                 */
                if (tempEntries.isNotEmpty() && tempEntries.last().x != it.x - 1) {
                    dataSets.add(LineDataSet(tempEntries, label))
                    tempEntries = mutableListOf()
                }
                tempEntries.add(it)
            }
        }
        /**
         * If we reached the end of the entries list and we have a pending LineDataSet to add
         */
        if (tempEntries.isNotEmpty()) {
            dataSets.add(LineDataSet(tempEntries, label))
        }

        return dataSets
    }

    fun getEntryValueForTooltip(position: Float): Float? {
        return entries.getOrNull(position.toInt())?.y?.takeIf { !it.isNaN() }
    }
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class WalletWarnings(
    val showMissingBadge: Boolean,
    val showMissingWarning: Boolean
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
data class DevicesRewardsByRange(
    val total: Float?,
    val mode: RewardsRepositoryImpl.Companion.RewardsSummaryMode?,
    val datesChartTooltip: List<String>,
    val lineChartData: LineChartData
)

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class DevicesRewards(
    val total: Float,
    val latest: Float,
    val devices: List<DeviceTotalRewards>
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class DeviceTotalRewards(
    val id: String,
    val name: String,
    val total: Float?,
    var details: DeviceTotalRewardsDetails
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class DeviceTotalRewardsDetails(
    val total: Float?,
    var mode: RewardsRepositoryImpl.Companion.RewardsSummaryMode?,
    val boosts: List<DeviceTotalRewardsBoost>,
    val totals: List<Float>,
    val datesChartTooltip: List<String>,
    val baseChartData: LineChartData,
    val betaChartData: LineChartData,
    val otherChartData: LineChartData,
    var status: Status
) : Parcelable {
    companion object {
        fun empty() = DeviceTotalRewardsDetails(
            null,
            RewardsRepositoryImpl.Companion.RewardsSummaryMode.WEEK,
            listOf(),
            listOf(),
            listOf(),
            LineChartData.empty(),
            LineChartData.empty(),
            LineChartData.empty(),
            Status.LOADING
        )
    }

    fun isEmpty(): Boolean {
        return total == null && mode == null && totals.isEmpty() &&
            boosts.isEmpty() && status == Status.LOADING
    }
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class DeviceTotalRewardsBoost(
    val boostCode: String?,
    val completedPercentage: Int?,
    val maxRewards: Float?,
    val currentRewards: Float?,
    val boostPeriodStart: ZonedDateTime?,
    val boostPeriodEnd: ZonedDateTime?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
data class PhotoExample(
    @DrawableRes val image: Int,
    val feedbackResId: List<Int>,
    val isGoodExample: Boolean
)

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class StationPhoto(
    val remotePath: String?,
    val localPath: String?,
    val source: PhotoSource? = null
) : Parcelable {
    val isLocal: Boolean
        get() = localPath != null
}

@Keep
data class WeatherUnit(
    val type: WeatherUnitType,
    val unit: String
)

@Keep
data class UploadPhotosState(
    val device: UIDevice,
    val progress: Int,
    val isSuccess: Boolean,
    val isError: Boolean,
    val isCancelled: Boolean = false
)

@Keep
@JsonClass(generateAdapter = true)
data class DataForMessageView(
    val extraTopPadding: Dp = 0.dp,
    val title: Int? = null,
    val subtitle: SubtitleForMessageView? = null,
    val drawable: Int? = null,
    val action: ActionForMessageView? = null,
    val useStroke: Boolean = false,
    val severityLevel: SeverityLevel = SeverityLevel.INFO,
    val onCloseListener: (() -> Unit)? = null
)

@Keep
@JsonClass(generateAdapter = true)
data class SubtitleForMessageView(
    val message: Int? = null,
    val htmlMessage: Int? = null,
    val htmlMessageAsString: String? = null,
    val onLinkClickedListener: (() -> Unit)? = null
)

@Keep
@JsonClass(generateAdapter = true)
data class ActionForMessageView(
    val label: Int,
    val backgroundTint: Int = R.color.translucent,
    val foregroundTint: Int = R.color.colorPrimary,
    val startIcon: Int? = null,
    val endIcon: Int? = null,
    val onClickListener: () -> Unit
)

enum class RewardTimelineType {
    DATA,
    END_OF_LIST
}

enum class PhotoSource {
    CAMERA,
    GALLERY;

    val exifUserComment: String
        get() = when (this) {
            CAMERA -> "wxm-device-photo-camera"
            GALLERY -> "wxm-device-photo-library"
        }
}

@Parcelize
enum class DevicesSortOrder : Parcelable {
    DATE_ADDED,
    NAME,
    LAST_ACTIVE
}

@Parcelize
enum class DevicesFilterType : Parcelable {
    ALL,
    OWNED,
    FAVORITES
}

@Parcelize
enum class DevicesGroupBy : Parcelable {
    NO_GROUPING,
    RELATIONSHIP,
    STATUS
}

@Parcelize
enum class AnnotationGroupCode : Parcelable {
    NO_WALLET,
    LOCATION_NOT_VERIFIED,
    NO_LOCATION_DATA,
    USER_RELOCATION_PENALTY,
    CELL_CAPACITY_REACHED,
    UNKNOWN
}

@Suppress("EnumNaming")
@Parcelize
enum class BundleName : Parcelable {
    m5,
    h1,
    h2,
    d1,
    pulse
}

@Parcelize
enum class ErrorType : Parcelable {
    WARNING,
    ERROR
}

enum class WeatherUnitType {
    CELSIUS,
    FAHRENHEIT,
    MILLIMETERS,
    INCHES,
    HPA,
    INHG,
    MS,
    BEAUFORT,
    KMH,
    MPH,
    KNOTS,
    CARDINAL,
    DEGREES
}

