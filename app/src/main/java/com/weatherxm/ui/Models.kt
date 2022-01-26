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
