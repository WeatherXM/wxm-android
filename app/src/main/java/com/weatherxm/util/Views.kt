package com.weatherxm.util

import android.content.Context
import android.content.res.Resources
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.StringRes
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.weatherxm.R
import com.weatherxm.ui.BarChartData
import com.weatherxm.ui.LineChartData
import com.weatherxm.ui.common.hide
import com.weatherxm.ui.common.show
import dev.chrisbanes.insetter.applyInsetter

private const val CHART_BOTTOM_OFFSET = 20F
private const val MINIMUM_VISIBLE_POINTS = 5F
private const val LINE_WIDTH = 2F
private const val POINT_SIZE = 2F
private const val MAXIMUMS_GRID_LINES_Y_AXIS = 4
private const val PRECIP_GRANULARITY_Y_AXIS = 0.1F
private const val TIME_GRANULARITY_X_AXIS = 3F

@Suppress("EmptyFunctionBlock")
fun EditText.onTextChanged(callback: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            callback(s.toString())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}

@Suppress("EmptyFunctionBlock")
fun TabLayout.onTabSelected(callback: (TabLayout.Tab) -> Unit) {
    this.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            tab?.let { callback(it) }
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {}
        override fun onTabReselected(tab: TabLayout.Tab?) {}
    })
}

fun Chip.setTextAndColor(@StringRes text: Int, color: Int) {
    this.setChipBackgroundColorResource(color)
    this.text = this.resources.getString(text)
}

fun ViewGroup.applyInsets(top: Boolean = true, bottom: Boolean = true) {
    this.applyInsetter {
        type(statusBars = top) {
            padding(left = false, top = true, right = false, bottom = false)
        }
        type(navigationBars = bottom) {
            padding(left = false, top = false, right = false, bottom = true)
        }
    }
}

private fun LineChart.setDefaultSettings() {
    // General Chart Settings
    description.isEnabled = false
    setVisibleXRangeMinimum(MINIMUM_VISIBLE_POINTS)
    extraBottomOffset = CHART_BOTTOM_OFFSET
    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER

    // Line and highlight Settings
    lineData.setDrawValues(false)

    // Y Axis settings
    axisLeft.isGranularityEnabled = true
    isScaleYEnabled = false
    axisRight.isEnabled = false
    axisLeft.gridColor = resources.getColor(R.color.chart_grid_color, context.theme)
    axisLeft.setLabelCount(MAXIMUMS_GRID_LINES_Y_AXIS, false)

    // X axis settings
    xAxis.position = XAxis.XAxisPosition.BOTTOM
    xAxis.setDrawAxisLine(false)
    xAxis.granularity = TIME_GRANULARITY_X_AXIS
    xAxis.gridColor = resources.getColor(R.color.chart_grid_color, context.theme)
}

private fun LineDataSet.setDefaultSettings(context: Context, resources: Resources) {
    setDrawCircleHole(false)
    circleRadius = POINT_SIZE
    lineWidth = LINE_WIDTH
    mode = LineDataSet.Mode.CUBIC_BEZIER
    highLightColor = resources.getColor(R.color.highlighter, context.theme)
}

fun LineChart.initializeDefault24hChart(chartData: LineChartData, yMinValue: Float?) {
    val dataSet = LineDataSet(chartData.entries, chartData.name)
    val lineData = LineData(dataSet)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings()

    // General Chart Settings
    legend.isEnabled = false
    marker =
        CustomDefaultMarkerView(
            context,
            chartData.timestamps,
            chartData.name,
            chartData.unit,
            chartData.showDecimals
        )

    // Line and highlight Settings
    dataSet.setDefaultSettings(context, resources)
    dataSet.color = resources.getColor(chartData.lineColor, context.theme)
    dataSet.setCircleColor(resources.getColor(chartData.lineColor, context.theme))

    // Y Axis settings
    axisLeft.valueFormatter = CustomYAxisFormatter(chartData.unit, false, 0)
    yMinValue?.let { axisLeft.axisMinimum = it }

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(chartData.timestamps)
    show()
    notifyDataSetChanged()
}

/*
    precipProbabilityData is nullable because we have that data only on Forecast Charts.
    On history chart this is null
*/
fun LineChart.initializePrecipitation24hChart(
    precipIntensityData: LineChartData,
    precipProbabilityData: LineChartData?
) {
    val dataSetPrecipIntensity = LineDataSet(precipIntensityData.entries, precipIntensityData.name)
    dataSetPrecipIntensity.axisDependency = AxisDependency.LEFT

    // use ILineDataSet to have multiple lines in a chart (in case we have probability)
    val dataSets = mutableListOf<ILineDataSet>()

    dataSets.add(dataSetPrecipIntensity)
    // Precipitation Probability setting up - only if we have the data (i.e. Forecast charts)
    precipProbabilityData?.let {
        val dataSetPrecipProbability = LineDataSet(it.entries, it.name)

        dataSetPrecipProbability.axisDependency = AxisDependency.RIGHT
        dataSetPrecipProbability.setDefaultSettings(context, resources)
        dataSetPrecipProbability.isHighlightEnabled = false
        dataSetPrecipProbability.color = resources.getColor(it.lineColor, context.theme)
        dataSetPrecipProbability.setCircleColor(resources.getColor(it.lineColor, context.theme))

        dataSets.add(dataSetPrecipProbability)
    }

    val lineData = LineData(dataSets)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings()

    // Marker view initialization based on if we will show precipitation probability data or not
    marker = if (precipProbabilityData == null) {
        CustomPrecipitationMarkerView(
            context,
            precipIntensityData.timestamps,
            null,
            precipIntensityData.name,
            null,
            precipIntensityData.unit
        )
    } else {
        CustomPrecipitationMarkerView(
            context,
            precipIntensityData.timestamps,
            precipProbabilityData.entries,
            precipIntensityData.name,
            precipProbabilityData.name,
            precipIntensityData.unit
        )
    }

    // Precipitation Intensity Settings
    dataSetPrecipIntensity.setDefaultSettings(context, resources)
    dataSetPrecipIntensity.mode = LineDataSet.Mode.STEPPED
    dataSetPrecipIntensity.setDrawFilled(true)
    dataSetPrecipIntensity.color = resources.getColor(precipIntensityData.lineColor, context.theme)
    dataSetPrecipIntensity.setCircleColor(
        resources.getColor(precipIntensityData.lineColor, context.theme)
    )

    // Y Axis settings
    axisLeft.granularity = PRECIP_GRANULARITY_Y_AXIS
    axisLeft.axisMinimum = dataSetPrecipIntensity.yMin
    axisLeft.valueFormatter =
        CustomYAxisFormatter(
            precipIntensityData.unit,
            precipIntensityData.showDecimals,
            Weather.getDecimalsPrecipitation()
        )
    // Precipitation Probability Y Axis Settings
    precipProbabilityData?.let {
        axisRight.isEnabled = true
        axisRight.valueFormatter = CustomYAxisFormatter(it.unit, it.showDecimals, 0)
        axisRight.setDrawGridLines(false)
        axisRight.axisMinimum = 0F
    }

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(precipIntensityData.timestamps)
    show()
    notifyDataSetChanged()
}

fun LineChart.initializeWind24hChart(
    windSpeedData: LineChartData, windGustData: LineChartData, windDirectionData: LineChartData
) {
    val dataSetWindSpeed = LineDataSet(windSpeedData.entries, windSpeedData.name)
    val dataSetWindGust = LineDataSet(windGustData.entries, windGustData.name)

    dataSetWindSpeed.axisDependency = AxisDependency.LEFT

    // use the interface ILineDataSet to have multiple lines in a chart
    val dataSets = mutableListOf<ILineDataSet>()
    dataSets.add(dataSetWindSpeed)
    dataSets.add(dataSetWindGust)
    val lineData = LineData(dataSets)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings()

    // General Chart Settings
    marker = CustomWindMarkerView(
        context,
        windSpeedData.timestamps,
        windGustData.entries,
        windDirectionData.entries,
        windSpeedData.name,
        windGustData.name,
        windSpeedData.unit
    )

    // Wind Speed
    dataSetWindSpeed.setDefaultSettings(context, resources)
    dataSetWindSpeed.color = resources.getColor(windSpeedData.lineColor, context.theme)
    dataSetWindSpeed.setCircleColor(resources.getColor(windSpeedData.lineColor, context.theme))

    // Wind Gust Settings
    dataSetWindGust.setDefaultSettings(context, resources)
    dataSetWindGust.setDrawIcons(false)
    dataSetWindGust.isHighlightEnabled = false
    dataSetWindGust.color = resources.getColor(windGustData.lineColor, context.theme)
    dataSetWindGust.setCircleColor(resources.getColor(windGustData.lineColor, context.theme))

    // Y Axis settings
    axisLeft.axisMinimum = dataSetWindSpeed.yMin
    axisLeft.valueFormatter =
        CustomYAxisFormatter(windSpeedData.unit, windSpeedData.showDecimals, 0)

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(windGustData.timestamps)
    show()
    notifyDataSetChanged()
}

fun BarChart.initializeDefault24hChart(data: BarChartData) {
    val dataSet = BarDataSet(data.entries, data.name)
    val barData = BarData(dataSet)
    setData(barData)

    // General Chart Settings
    description.isEnabled = false
    legend.isEnabled = false
    marker =
        CustomDefaultMarkerView(context, data.timestamps, data.name, data.unit, data.showDecimals)
    setVisibleXRangeMinimum(MINIMUM_VISIBLE_POINTS)
    extraBottomOffset = CHART_BOTTOM_OFFSET

    // Bar and highlight Settings
    barData.setDrawValues(false)
    dataSet.color = resources.getColor(R.color.uvIndex, context.theme)
    dataSet.highLightColor = resources.getColor(R.color.highlighter, context.theme)

    // Y Axis settings
    axisLeft.isGranularityEnabled = true
    axisLeft.granularity = 1F
    axisLeft.valueFormatter = CustomYAxisFormatter(data.unit, false, 0)
    axisRight.isEnabled = false
    isScaleYEnabled = false

    // X axis settings
    xAxis.position = XAxis.XAxisPosition.BOTTOM
    xAxis.setDrawAxisLine(false)
    xAxis.setDrawGridLines(false)
    xAxis.valueFormatter = CustomXAxisFormatter(data.timestamps)
    xAxis.granularity = TIME_GRANULARITY_X_AXIS
    show()
    notifyDataSetChanged()
}

fun FloatingActionButton.showIfNot() {
    if (this.isOrWillBeHidden) {
        this.show()
    }
}

fun FloatingActionButton.hideIfNot() {
    if (this.isOrWillBeShown) {
        this.hide()
    }
}

fun BottomNavigationView.showIfNot() {
    if (!this.isShown) {
        this.show()
    }
}

fun BottomNavigationView.hideIfNot() {
    if (this.isShown) {
        this.hide()
    }
}
