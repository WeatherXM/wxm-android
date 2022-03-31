package com.weatherxm.ui

import androidx.annotation.Keep
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.squareup.moshi.JsonClass
import com.weatherxm.data.Transaction
import com.weatherxm.data.HourlyWeather

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
    var cloudCover: LineChartData,
    var pressure: LineChartData,
    var uvIndex: BarChartData
) {
    fun isEmpty(): Boolean {
        return temperature.isNullOrEmpty() && precipitation.isNullOrEmpty()
            && windSpeed.isNullOrEmpty() && windGust.isNullOrEmpty()
            && windDirection.isNullOrEmpty() && humidity.isNullOrEmpty()
            && cloudCover.isNullOrEmpty() && pressure.isNullOrEmpty() && uvIndex.isNullOrEmpty()
    }
}

@Keep
@JsonClass(generateAdapter = true)
data class LineChartData(
    var name: String,
    var lineColor: Int,
    var unit: String,
    var showDecimals: Boolean = false,
    var timestamps: MutableList<String>,
    var entries: MutableList<Entry>
) {
    fun isNullOrEmpty(): Boolean {
        return timestamps.isNullOrEmpty() && entries.isNullOrEmpty()
    }
}

@Keep
@JsonClass(generateAdapter = true)
data class BarChartData(
    var name: String,
    var lineColor: Int,
    var unit: String,
    var showDecimals: Boolean = false,
    var timestamps: MutableList<String>,
    var entries: MutableList<BarEntry>
) {
    fun isNullOrEmpty(): Boolean {
        return timestamps.isNullOrEmpty() && entries.isNullOrEmpty()
    }
}

@Keep
@JsonClass(generateAdapter = true)
data class TokenData(
    var tokens24h: TokenSummary,
    var tokens7d: TokenSummary,
    var tokens30d: TokenSummary
)

@Keep
@JsonClass(generateAdapter = true)
data class TokenSummary(
    var total: Float,
    var values: MutableList<Pair<String, Float>>
)

@Keep
@JsonClass(generateAdapter = true)
data class ForecastData(
    var minTemp: Float?,
    var maxTemp: Float?,
    var dailyForecasts: List<DailyForecast>
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
    var transactions: List<Transaction>,
    var hasNextPage: Boolean
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
