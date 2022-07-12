package com.weatherxm.ui

import androidx.annotation.Keep
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.squareup.moshi.JsonClass
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.Transaction

@Keep
data class UIError(
    var errorMessage: String,
    var retryFunction: (() -> Unit)?
)

@Keep
@JsonClass(generateAdapter = true)
data class HistoryCharts(
    var date: String?,
    var temperature: LineChartData,
    var precipitation: LineChartData,
    var windSpeed: LineChartData,
    var windGust: LineChartData,
    var windDirection: LineChartData,
    var humidity: LineChartData,
    var pressure: LineChartData,
    var uvIndex: BarChartData
) {
    fun isEmpty(): Boolean {
        return temperature.isNullOrEmpty() && precipitation.isNullOrEmpty()
            && windSpeed.isNullOrEmpty() && windGust.isNullOrEmpty()
            && windDirection.isNullOrEmpty() && humidity.isNullOrEmpty()
            && pressure.isNullOrEmpty() && uvIndex.isNullOrEmpty()
    }
}

@Keep
@JsonClass(generateAdapter = true)
data class LineChartData(
    var name: String,
    var lineColor: Int,
    var unit: String,
    var timestamps: MutableList<String>,
    var entries: MutableList<Entry>
) {
    fun isNullOrEmpty(): Boolean {
        return timestamps.isEmpty() && entries.isEmpty()
    }
}

@Keep
@JsonClass(generateAdapter = true)
data class BarChartData(
    var name: String,
    var lineColor: Int,
    var unit: String,
    var timestamps: MutableList<String>,
    var entries: MutableList<BarEntry>
) {
    fun isNullOrEmpty(): Boolean {
        return timestamps.isEmpty() && entries.isEmpty()
    }
}

@Keep
@JsonClass(generateAdapter = true)
data class TokenInfo(
    var lastReward: Transaction? = null,
    var total7d: Float? = null,
    var chart7d: TokenValuesChart? = null,
    var max7dReward: Float? = null,
    var total30d: Float? = null,
    var chart30d: TokenValuesChart? = null,
    var max30dReward: Float? = null
)

@Keep
@JsonClass(generateAdapter = true)
data class TokenValuesChart(
    var values: MutableList<Pair<String, Float>>
)

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
