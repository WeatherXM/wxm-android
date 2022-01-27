package com.weatherxm.ui

import androidx.annotation.Keep
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.squareup.moshi.JsonClass

@Keep
data class UIError(
    var errorMessage: String,
    var retryFunction: (() -> Unit)?
)

@Keep
@JsonClass(generateAdapter = true)
data class HistoryCharts(
    var temperature: LineChartData,
    var precipitation: LineChartData,
    var windSpeed: LineChartData,
    var windGust: LineChartData,
    var windDirection: LineChartData,
    var humidity: LineChartData,
    var cloudCover: LineChartData,
    var pressure: LineChartData,
    var uvIndex: BarChartData
)

@Keep
@JsonClass(generateAdapter = true)
data class LineChartData(
    var name: String,
    var lineColor: Int,
    var unit: String,
    var showDecimals: Boolean,
    var timestamps: MutableList<String>,
    var entries: MutableList<Entry>
)

@Keep
@JsonClass(generateAdapter = true)
data class BarChartData(
    var name: String,
    var lineColor: Int,
    var unit: String,
    var showDecimals: Boolean,
    var timestamps: MutableList<String>,
    var entries: MutableList<BarEntry>
)

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
    var nameOfDay: String,
    var dateOfDay: String,
    var icon: String?,
    var minTemp: Float?,
    var maxTemp: Float?
)
