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
    var ownershipStatus: DeviceOwnershipStatus?,
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
    val timezone: String?,
    var address: String?,
    @Json(name = "current_weather")
    val currentWeather: HourlyWeather?,
    var rewardsInfo: RewardsInfo?,
    var alerts: List<DeviceAlert> = listOf()
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
enum class DeviceOwnershipStatus : Parcelable {
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


