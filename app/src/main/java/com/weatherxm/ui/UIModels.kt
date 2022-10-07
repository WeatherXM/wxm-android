package com.weatherxm.ui

import android.os.Parcelable
import androidx.annotation.Keep
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.LastAndDatedTxs
import com.weatherxm.data.Location
import com.weatherxm.data.Transaction
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
import java.time.ZonedDateTime

@Keep
data class UIError(
    var errorMessage: String,
    var retryFunction: (() -> Unit)? = null
)

@Keep
@JsonClass(generateAdapter = true)
data class HistoryCharts(
    var date: LocalDate,
    var temperature: LineChartData,
    var feelsLike: LineChartData,
    var precipitation: LineChartData,
    var windSpeed: LineChartData,
    var windGust: LineChartData,
    var windDirection: LineChartData,
    var humidity: LineChartData,
    var pressure: LineChartData,
    var uvIndex: BarChartData
) {
    fun isEmpty(): Boolean {
        return temperature.isNullOrEmpty() && feelsLike.isNullOrEmpty()
            && precipitation.isNullOrEmpty() && windSpeed.isNullOrEmpty()
            && windGust.isNullOrEmpty() && windDirection.isNullOrEmpty()
            && humidity.isNullOrEmpty() && pressure.isNullOrEmpty() && uvIndex.isNullOrEmpty()
    }
}

@Keep
@JsonClass(generateAdapter = true)
data class LineChartData(
    var name: String,
    var unit: String,
    var timestamps: MutableList<String>,
    var entries: MutableList<Entry>
) {
    fun isNullOrEmpty(): Boolean {
        return timestamps.isEmpty() && entries.isEmpty()
    }

    fun isDataValid(): Boolean {
        return timestamps.isNotEmpty() && entries.isNotEmpty()
    }
}

@Keep
@JsonClass(generateAdapter = true)
data class BarChartData(
    var name: String,
    var unit: String,
    var timestamps: MutableList<String>,
    var entries: MutableList<BarEntry>
) {
    fun isNullOrEmpty(): Boolean {
        return timestamps.isEmpty() && entries.isEmpty()
    }

    fun isDataValid(): Boolean {
        return timestamps.isNotEmpty() && entries.isNotEmpty()
    }
}

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
data class DailyForecast(
    var nameOfDay: String = "",
    var dateOfDay: String = "",
    var icon: String? = null,
    var minTemp: Float? = null,
    var maxTemp: Float? = null,
    var precipProbability: Int? = null
)

@Keep
@JsonClass(generateAdapter = true)
data class UITransactions(
    var uiTransactions: List<UITransaction>,
    var hasNextPage: Boolean = false,
    var reachedTotal: Boolean = false
)

@Keep
@JsonClass(generateAdapter = true)
data class UITransaction(
    val formattedDate: String,
    val formattedTimestamp: String,
    val txHash: String?,
    val txHashMasked: String?,
    val validationScore: Float?,
    val dailyReward: Float?,
    val actualReward: Float?,
)

@Keep
@JsonClass(generateAdapter = true)
data class ProfileInfo(
    var email: String = "",
    var name: String? = null,
    var walletAddress: String? = null,
)

@Keep
@JsonClass(generateAdapter = true)
data class SelectedHourlyForecast(
    var hourlyWeather: HourlyWeather,
    var selectedPosition: Int
)

@Keep
@JsonClass(generateAdapter = true)
data class ExplorerData(
    var totalDevices: Int,
    var geoJsonSource: GeoJsonSource,
    var polygonPoints: List<PolygonAnnotationOptions>,
)

@Keep
@JsonClass(generateAdapter = true)
data class UIHex(
    var index: String,
    var center: Location
)

@Keep
@JsonClass(generateAdapter = true)
data class ExplorerCamera(
    var zoom: Double,
    var center: Point
)

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class UIDevice(
    val id: String,
    val name: String,
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
    val eui: String?,
    val type: DeviceType = DeviceType.HELIUM
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class AvailableDeviceType(
    val title: String,
    val desc: String,
    val type: DeviceType,
) : Parcelable

enum class DeviceType {
    M5_WIFI,
    HELIUM
}
