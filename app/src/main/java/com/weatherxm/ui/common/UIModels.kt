package com.weatherxm.ui.common

import android.content.Context
import android.os.Parcelable
import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.weatherxm.R
import com.weatherxm.data.DeviceProfile
import com.weatherxm.data.Hex
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.Location
import com.weatherxm.data.QoDErrorAffects
import com.weatherxm.data.RewardsAnnotations
import com.weatherxm.data.RewardsObject
import com.weatherxm.data.Transaction
import com.weatherxm.util.Analytics
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.DateTimeHelper.getFormattedDay
import com.weatherxm.util.DateTimeHelper.getFormattedTime
import com.weatherxm.util.Rewards.shouldHideAnnotations
import com.weatherxm.util.isToday
import com.weatherxm.util.isYesterday
import kotlinx.parcelize.Parcelize
import java.time.ZoneId
import java.time.ZonedDateTime

@Keep
data class UIError(
    var errorMessage: String,
    var errorCode: String? = null,
    var retryFunction: (() -> Unit)? = null
)

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class UIRewards(
    val allTimeRewards: Float? = null,
    var latest: UIRewardObject? = null,
    var weekly: UIRewardObject? = null,
    var monthly: UIRewardObject? = null,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class UIRewardObject(
    var rewardTimestamp: ZonedDateTime? = null,
    var rewardFormattedTimestamp: String? = null,
    var rewardFormattedDate: String? = null,
    var fromDate: String? = null,
    var toDate: String? = null,
    var actualReward: Float? = null,
    var lostRewards: Float? = null,
    var rewardScore: Int? = null,
    var periodMaxReward: Float? = null,
    var timelineTitle: String? = null,
    var timelineScores: List<Int> = emptyList(),
    var annotations: MutableList<UIRewardsAnnotation> = mutableListOf()
) : Parcelable {
    constructor(
        context: Context,
        rewards: RewardsObject,
        hideAnnotationsThreshold: Long,
        isRange: Boolean = false
    ) : this() {
        val utcFromDate =
            rewards.fromDate?.withZoneSameInstant(ZoneId.of("UTC"))?.getFormattedDate()
        val utcToDate =
            rewards.toDate?.withZoneSameInstant(ZoneId.of("UTC"))?.getFormattedDate()
        rewardFormattedTimestamp = if (isRange) {
            "$utcFromDate - $utcToDate (UTC)"
        } else {
            rewards.timestamp?.let { timestamp ->
                val utcTimestamp = timestamp.withZoneSameInstant(ZoneId.of("UTC"))
                rewardTimestamp = timestamp
                val date = utcTimestamp.getFormattedDate()
                val time = utcTimestamp.getFormattedTime(context)
                "$date, $time (UTC)"
            }
        }

        fromDate = utcFromDate
        toDate = utcToDate
        actualReward = rewards.actualReward
        lostRewards = rewards.lostRewards
        rewardScore = rewards.rewardScore
        periodMaxReward = rewards.periodMaxReward
        setTimelineTitle(context, rewards.timeline?.referenceDate, fromDate, toDate)
        timelineScores = rewards.timeline?.rewardScores ?: mutableListOf()
        setAnnotations(hideAnnotationsThreshold, rewards.annotations)
    }

    constructor(context: Context, tx: Transaction, hideAnnotationsThreshold: Long) : this() {
        this.rewardTimestamp = tx.timestamp
        val date = tx.timestamp.getFormattedDate()
        val time = tx.timestamp.getFormattedTime(context)
        rewardFormattedTimestamp = "$date, $time (UTC)"
        this.rewardFormattedDate = tx.timestamp.getFormattedDate(true)
        actualReward = tx.actualReward
        lostRewards = tx.lostRewards
        rewardScore = tx.rewardScore
        periodMaxReward = tx.dailyReward
        setTimelineTitle(context, tx.timeline?.referenceDate)
        timelineScores = tx.timeline?.rewardScores ?: mutableListOf()
        setAnnotations(hideAnnotationsThreshold, tx.annotations)
    }

    private fun setTimelineTitle(
        context: Context,
        referenceDate: ZonedDateTime?,
        fromDate: String? = null,
        toDate: String? = null
    ) {
        val dateToShow = referenceDate?.let {
            val todayOrYesterday = if (it.isToday() || it.isYesterday()) {
                it.getFormattedDay(context)
            } else {
                null
            }
            val timelineDate = it.getFormattedDate(true)
            if (todayOrYesterday.isNullOrEmpty()) {
                timelineDate
            } else {
                "$todayOrYesterday, $timelineDate"
            }
        } ?: kotlin.run {
            if (fromDate != null && toDate != null) {
                "$fromDate - $toDate"
            } else {
                null
            }
        }
        dateToShow?.let {
            timelineTitle = context.getString(R.string.timeline_for, it)
        }
    }

    private fun setAnnotations(
        hideAnnotationsThreshold: Long,
        annotations: RewardsAnnotations?
    ) {
        this.annotations = mutableListOf()
        annotations?.pol?.forEach {
            this.annotations.add(UIRewardsAnnotation(it.toAnnotationCode(), it.ratio))
        }
        annotations?.rm?.forEach {
            this.annotations.add(UIRewardsAnnotation(it.toAnnotationCode(), it.ratio))
        }
        if (!shouldHideAnnotations(rewardScore, hideAnnotationsThreshold)) {
            annotations?.qod?.forEach {
                val qodRewardsAnnotation = UIRewardsAnnotation(it.toAnnotationCode(), it.ratio)
                it.affects?.let { affectedParameters ->
                    qodRewardsAnnotation.qodParametersAffected = affectedParameters
                }
                this.annotations.add(qodRewardsAnnotation)
            }
        }
    }
}

@Keep
@JsonClass(generateAdapter = true)
data class UIRewardsList(
    var rewards: List<UIRewardObject>,
    var hasNextPage: Boolean = false,
    var reachedTotal: Boolean = false
)

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
    val friendlyName: String?,
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
    var alerts: List<DeviceAlert> = listOf(),
    val isDeviceFromSearchResult: Boolean = false
) : Parcelable {
    companion object {
        fun empty() = UIDevice(
            "",
            "",
            "",
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

    fun needsUpdate(): Boolean {
        return !currentFirmware.equals(assignedFirmware) && !assignedFirmware.isNullOrEmpty()
    }

    fun getDefaultOrFriendlyName(): String {
        return friendlyName ?: name
    }

    fun getLastCharsOfLabel(charCount: Int): String {
        val cleanLabel = label?.replace(":", "")
        return cleanLabel?.substring(cleanLabel.length - charCount, cleanLabel.length) ?: ""
    }

    fun toNormalizedName(): String {
        return name.replace(" ", "-").lowercase()
    }

    fun isEmpty() = id.isEmpty() && name.isEmpty() && cellIndex.isEmpty()

    fun isOnline() = isActive != null && isActive == true
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
        fun empty() = ScannedDevice(
            "", ""
        )
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
    HELIUM
}

@Parcelize
enum class DeviceRelation : Parcelable {
    OWNED,
    FOLLOWED,
    UNFOLLOWED
}

@Parcelize
enum class DeviceAlert : Parcelable {
    OFFLINE,
    NEEDS_UPDATE
}

@Keep
@JsonClass(generateAdapter = true)
data class UIForecast(
    var nameOfDayAndDate: String = "",
    var icon: String? = null,
    var minTemp: Float? = null,
    var maxTemp: Float? = null,
    var precipProbability: Int? = null,
    var precip: Float? = null,
    var windSpeed: Float? = null,
    var windDirection: Int? = null,
    var humidity: Int? = null,
    var hourlyWeather: List<HourlyWeather>?
)

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
                it.relation == DeviceRelation.OWNED
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
            DevicesSortOrder.DATE_ADDED -> Analytics.ParamValue.FILTERS_SORT_DATE_ADDED.paramValue
            DevicesSortOrder.NAME -> Analytics.ParamValue.FILTERS_SORT_NAME.paramValue
            DevicesSortOrder.LAST_ACTIVE -> Analytics.ParamValue.FILTERS_SORT_LAST_ACTIVE.paramValue
        }
    }

    fun getFilterAnalyticsValue(): String {
        return when (filterType) {
            DevicesFilterType.ALL -> Analytics.ParamValue.FILTERS_FILTER_ALL.paramValue
            DevicesFilterType.OWNED -> Analytics.ParamValue.FILTERS_FILTER_OWNED.paramValue
            DevicesFilterType.FAVORITES -> Analytics.ParamValue.FILTERS_FILTER_FAVORITES.paramValue
        }
    }

    fun getGroupByAnalyticsValue(): String {
        return when (groupBy) {
            DevicesGroupBy.NO_GROUPING -> Analytics.ParamValue.FILTERS_GROUP_NO_GROUPING.paramValue
            DevicesGroupBy.RELATIONSHIP -> {
                Analytics.ParamValue.FILTERS_GROUP_RELATIONSHIP.paramValue
            }
            DevicesGroupBy.STATUS -> Analytics.ParamValue.FILTERS_GROUP_STATUS.paramValue
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
        fun empty() = UIWalletRewards(0.0, 0.0, 0.0, "")
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
