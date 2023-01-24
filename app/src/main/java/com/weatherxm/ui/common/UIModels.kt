package com.weatherxm.ui.common

import android.os.Parcelable
import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.weatherxm.data.DeviceProfile
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.LastAndDatedTxs
import com.weatherxm.data.Transaction
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@Keep
data class UIError(
    var errorMessage: String,
    var retryFunction: (() -> Unit)? = null
)

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class TokenInfo(
    var lastReward: Transaction? = null,
    var total7d: Float = 0.0F,
    var chart7d: TokenValuesChart = TokenValuesChart(mutableListOf()),
    var max7dReward: Float? = null,
    var total30d: Float = 0.0F,
    var chart30d: TokenValuesChart = TokenValuesChart(mutableListOf()),
    var max30dReward: Float? = null
) : Parcelable {
    @Suppress("MagicNumber")
    fun fromLastAndDatedTxs(lastAndDatedTxs: LastAndDatedTxs): TokenInfo {
        lastReward = lastAndDatedTxs.lastTx
        total7d = 0.0F
        total30d = 0.0F
        chart7d = TokenValuesChart(mutableListOf())
        chart30d = TokenValuesChart(mutableListOf())

        /*
        * Populate the totals and the chart data from latest -> earliest
         */
        for ((position, datedTx) in lastAndDatedTxs.datedTxs.withIndex()) {
            if (position <= 6) {
                if (datedTx.second > Transaction.VERY_SMALL_NUMBER_FOR_CHART) {
                    total7d = total7d.plus(datedTx.second)
                }
                chart7d.values.add(datedTx)
            }
            if (datedTx.second > Transaction.VERY_SMALL_NUMBER_FOR_CHART) {
                total30d = total30d.plus(datedTx.second)
            }
            chart30d.values.add(datedTx)
        }

        // Find the maximum 7 and 30 day rewards (AKA the biggest bar on the chart)
        max7dReward = chart7d.values.maxOfOrNull { it.second }
        max30dReward = chart30d.values.maxOfOrNull { it.second }

        /*
        * We need to reverse the order in the chart data because we have saved them
        * from latest -> earliest but we need the earliest -> latest for proper
        * displaying them
        */
        chart7d.values = chart7d.values.reversed().toMutableList()
        chart30d.values = chart30d.values.reversed().toMutableList()

        return this
    }
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class TokenValuesChart(
    var values: MutableList<Pair<String, Float>>
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class UIDevice(
    val id: String,
    val name: String,
    var profile: DeviceProfile?,
    val cellIndex: String?,
    val isActive: Boolean?,
    val lastWeatherStationActivity: ZonedDateTime?,
    val timezone: String?,
    var address: String?,
    @Json(name = "current_weather")
    val currentWeather: HourlyWeather?,
    var tokenInfo: TokenInfo?
) : Parcelable

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

@Parcelize
enum class DeviceType : Parcelable {
    M5_WIFI,
    HELIUM
}
