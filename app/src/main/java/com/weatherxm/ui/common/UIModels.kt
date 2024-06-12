package com.weatherxm.ui.common

import android.os.Parcelable
import androidx.annotation.Keep
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.DeviceProfile
import com.weatherxm.data.Hex
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.Location
import com.weatherxm.data.QoDErrorAffects
import com.weatherxm.data.Reward
import com.weatherxm.data.SeverityLevel
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
    var hasNextPage: Boolean = false,
    var reachedTotal: Boolean = false
)

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class TimelineReward(
    val type: RewardTimelineType,
    val data: Reward?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class UIRewardsAnnotation(
    var annotation: AnnotationCode?,
    var ratioOfAnnotation: Int? = null,
    var qodParametersAffected: List<QoDErrorAffects> = emptyList(),
) : Parcelable {
    fun getAffectedParameters(): String {
        return qodParametersAffected
            .map {
                it.parameter?.replace("_", " ")
            }
            .joinToString(", ")
    }
}

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
    var profile: DeviceProfile?,
    val location: Location?,
    var cellCenter: Location?,
    var hex7: Hex?,
    val isActive: Boolean?,
    val currentFirmware: String?,
    val assignedFirmware: String?,
    val claimedAt: ZonedDateTime?,
    val lastWeatherStationActivity: ZonedDateTime?,
    var timezone: String?,
    var address: String?,
    @Json(name = "current_weather")
    val currentWeather: HourlyWeather?,
    val hasLowBattery: Boolean?,
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
            && profile == DeviceProfile.Helium
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

    fun createDeviceAlerts(userShouldNotifiedOfOTA: Boolean): List<DeviceAlert> {
        val alerts = mutableListOf<DeviceAlert>()
        if (isActive == false) {
            alerts.add(DeviceAlert.createError(DeviceAlertType.OFFLINE))
        }

        if (hasLowBattery == true && isOwned()) {
            alerts.add(DeviceAlert.createWarning(DeviceAlertType.LOW_BATTERY))
        }

        if (userShouldNotifiedOfOTA && shouldPromptUpdate()) {
            alerts.add(DeviceAlert.createWarning(DeviceAlertType.NEEDS_UPDATE))
        }
        this.alerts = alerts.sortedByDescending { alert ->
            alert.severity
        }
        return alerts
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
@JsonClass(generateAdapter = true)
@Parcelize
data class ScannedDevice(
    val address: String,
    val name: String?,
    val type: DeviceType = DeviceType.HELIUM
) : Parcelable {
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
    NEEDS_UPDATE
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
        /**
         * TODO: When we have the "date added" field on followed devices apply sorting here.
         */
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

    fun areDefaultFiltersOn(): Boolean {
        return sortOrder == DevicesSortOrder.DATE_ADDED &&
            filterType == DevicesFilterType.ALL &&
            groupBy == DevicesGroupBy.NO_GROUPING
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
data class MainnetInfo(
    val message: String,
    val url: String
) : Parcelable

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
data class LineChartData(
    var timestamps: MutableList<String>,
    var entries: MutableList<Entry>
) {
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
}

@Suppress("EnumNaming")
enum class RewardTimelineType {
    DATA,
    END_OF_LIST
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
enum class AnnotationCode : Parcelable {
    OBC,
    SPIKE_INST,
    NO_DATA,
    NO_MEDIAN,
    SHORT_CONST,
    LONG_CONST,
    FROZEN_SENSOR,
    ANOMALOUS_INCREASE,
    LOCATION_NOT_VERIFIED,
    NO_LOCATION_DATA,
    NO_WALLET,
    CELL_CAPACITY_REACHED,
    RELOCATED,
    POL_THRESHOLD_NOT_REACHED,
    QOD_THRESHOLD_NOT_REACHED,
    UNIDENTIFIED_SPIKE,
    UNIDENTIFIED_ANOMALOUS_CHANGE,
    UNKNOWN
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
