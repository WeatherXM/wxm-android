package com.weatherxm.util

import android.content.Context
import android.content.res.Resources
import android.view.MotionEvent
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.weatherxm.R
import com.weatherxm.ui.BarChartData
import com.weatherxm.ui.LineChartData
import com.weatherxm.ui.common.show

private const val CHART_BOTTOM_OFFSET = 20F
private const val LINE_WIDTH = 2F
private const val POINT_SIZE = 2F
private const val MAXIMUMS_GRID_LINES_Y_AXIS = 4
private const val PRECIP_GRANULARITY_Y_AXIS = 0.1F
private const val DEFAULT_GRANULARITY_Y_AXIS = 0.1F
private const val INHG_GRANULARITY_Y_AXIS = 0.01F
private const val TIME_GRANULARITY_X_AXIS = 3F

private fun LineChart.setDefaultSettings() {
    // General Chart Settings
    description.isEnabled = false
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

    setOnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                highlightValue(null)
            }
        }
        this.performClick()
    }
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

    // If max - min < 2 that means that the values are probably too close together.
    // Which causes a bug not showing labels on Y axis because granularity is set 1.
    // So this is a custom fix to change that granularity and show decimals at the Y labels
    if (dataSet.yMax - dataSet.yMin < 2 && chartData.showDecimals) {
        axisLeft.granularity = DEFAULT_GRANULARITY_Y_AXIS
        axisLeft.valueFormatter = CustomYAxisFormatter(
            chartData.unit,
            showDecimals = true,
            decimals = 1
        )
    } else {
        axisLeft.valueFormatter = CustomYAxisFormatter(
            chartData.unit,
            showDecimals = false,
            decimals = 0
        )
    }

    yMinValue?.let { axisLeft.axisMinimum = it }

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(chartData.timestamps)
    show()
    notifyDataSetChanged()
}

fun LineChart.initializePressure24hChart(chartData: LineChartData, yMinValue: Float?) {
    val dataSet = LineDataSet(chartData.entries, chartData.name)
    val lineData = LineData(dataSet)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings()

    // In History, if hPa show 1 decimal, if inHg show 2, so we need this variable for such cases
    val inHgUsed = Weather.getPreferredUnit(
        resources.getString(R.string.key_pressure_preference),
        resources.getString(R.string.pressure_hpa)
    ) == resources.getString(R.string.pressure_inHg)

    // General Chart Settings
    legend.isEnabled = false
    val decimalsOnMarkerView = if (inHgUsed) 2 else 1
    marker =
        CustomDefaultMarkerView(
            context,
            chartData.timestamps,
            chartData.name,
            chartData.unit,
            chartData.showDecimals,
            decimals = decimalsOnMarkerView
        )

    // Line and highlight Settings
    dataSet.setDefaultSettings(context, resources)
    dataSet.color = resources.getColor(chartData.lineColor, context.theme)
    dataSet.setCircleColor(resources.getColor(chartData.lineColor, context.theme))

    // Y Axis settings

    /*
    * If max - min < 2 that means that the values are probably too close together.
    * Which causes a bug not showing labels on Y axis because granularity is set 1.
    * So this is a custom fix to change that granularity and show decimals at the Y labels.
    * Also custom fix if inHg is used to show 2 decimals instead of one, for the same reason
    */
    if (dataSet.yMax - dataSet.yMin < 2 && chartData.showDecimals && !inHgUsed) {
        axisLeft.granularity = DEFAULT_GRANULARITY_Y_AXIS
        axisLeft.valueFormatter = CustomYAxisFormatter(
            chartData.unit,
            showDecimals = true,
            decimals = 1
        )
    } else if (inHgUsed) {
        axisLeft.granularity = INHG_GRANULARITY_Y_AXIS
        axisLeft.valueFormatter = CustomYAxisFormatter(
            chartData.unit,
            showDecimals = true,
            decimals = 2
        )
    } else {
        axisLeft.valueFormatter = CustomYAxisFormatter(
            chartData.unit,
            showDecimals = false,
            decimals = 0
        )
    }

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
fun LineChart.initializePrecipitation24hChart(precipIntensityData: LineChartData) {
    val dataSetPrecipIntensity = LineDataSet(precipIntensityData.entries, precipIntensityData.name)
    dataSetPrecipIntensity.axisDependency = YAxis.AxisDependency.LEFT

    // use ILineDataSet to have multiple lines in a chart (in case we have probability)
    val dataSets = mutableListOf<ILineDataSet>()

    dataSets.add(dataSetPrecipIntensity)

    val lineData = LineData(dataSets)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings()

    // Marker view initialization
    marker = CustomDefaultMarkerView(
        context,
        precipIntensityData.timestamps,
        precipIntensityData.name,
        precipIntensityData.unit,
        true,
        isPrecipitation = true
    )

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

    dataSetWindSpeed.axisDependency = YAxis.AxisDependency.LEFT

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
        CustomYAxisFormatter(
            windSpeedData.unit,
            windSpeedData.showDecimals,
            0
        )

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
    extraBottomOffset = CHART_BOTTOM_OFFSET

    // Bar and highlight Settings
    barData.setDrawValues(false)
    dataSet.color = resources.getColor(R.color.uvIndex, context.theme)
    dataSet.highLightColor = resources.getColor(R.color.highlighter, context.theme)

    setOnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                highlightValue(null)
            }
        }
        this.performClick()
    }

    // Y Axis settings
    axisLeft.axisMinimum = 0F
    axisLeft.isGranularityEnabled = true
    axisLeft.granularity = 1F
    axisLeft.valueFormatter =
        CustomYAxisFormatter(data.unit, showDecimals = false, decimals = 0)
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
