package com.weatherxm.ui.common

import android.os.Parcelable
import androidx.annotation.Keep
import com.github.mikephil.charting.data.BarEntry
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.weatherxm.data.DeviceProfile
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.LastAndDatedTxs
import com.weatherxm.data.Location
import com.weatherxm.data.Transaction
import com.weatherxm.util.Analytics
import kotlinx.parcelize.Parcelize
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
data class RewardsInfo(
    var lastReward: Transaction? = null,
    var chartTimestamps: MutableList<String> = mutableListOf(),
    var chart7dEntries: MutableList<BarEntry> = mutableListOf(),
    var total7d: Float = 0.0F,
    var max7dReward: Float? = null,
    var total30d: Float = 0.0F,
    var chart30dEntries: MutableList<BarEntry> = mutableListOf(),
    var max30dReward: Float? = null,
    val totalRewards: Float? = null
) : Parcelable {
    @Suppress("MagicNumber")
    fun fromLastAndDatedTxs(lastAndDatedTxs: LastAndDatedTxs): RewardsInfo {
        lastReward = lastAndDatedTxs.lastTx
        total7d = 0.0F
        total30d = 0.0F
        chart7dEntries = mutableListOf()
        chart30dEntries = mutableListOf()

        /*
        * Populate the totals and the chart data from latest -> earliest
        * We need to reverse the order in the chart data because we have saved them
        * from latest -> earliest but we need the earliest -> latest for proper
        * displaying them as bars
        */
        val reversedLastAndDatedTxs = lastAndDatedTxs.datedTxs.reversed()

        for ((position, datedTx) in reversedLastAndDatedTxs.withIndex()) {
            if (position in 23..30) {
                if (datedTx.second > Transaction.VERY_SMALL_NUMBER_FOR_CHART) {
                    total7d = total7d.plus(datedTx.second)
                }
                chart7dEntries.add(BarEntry(position.toFloat(), datedTx.second))
            }
            if (datedTx.second > Transaction.VERY_SMALL_NUMBER_FOR_CHART) {
                total30d = total30d.plus(datedTx.second)
            }
            chart30dEntries.add(BarEntry(position.toFloat(), datedTx.second))
            chartTimestamps.add(datedTx.first)
        }

        // Find the maximum 7 and 30 day rewards (AKA the biggest bar on the chart)
        max7dReward = chart7dEntries.maxOfOrNull { it.y }
        max30dReward = chart30dEntries.maxOfOrNull { it.y }

        return this
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
    val isActive: Boolean?,
    val currentFirmware: String?,
    val assignedFirmware: String?,
    val claimedAt: ZonedDateTime?,
    val lastWeatherStationActivity: ZonedDateTime?,
    var timezone: String?,
    var address: String?,
    @Json(name = "current_weather")
    val currentWeather: HourlyWeather?,
    var rewardsInfo: RewardsInfo?,
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
        return cleanLabel?.substring((cleanLabel.length - charCount), cleanLabel.length) ?: ""
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


