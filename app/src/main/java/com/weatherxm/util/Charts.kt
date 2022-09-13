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
private const val Y_AXIS_1_DECIMAL_GRANULARITY = 0.1F
private const val Y_AXIS_PRECIP_INCHES_GRANULARITY = 0.01F
private const val Y_AXIS_PRESSURE_INHG_GRANULARITY = 0.01F
private const val X_AXIS_DEFAULT_TIME_GRANULARITY = 3F
private const val X_AXIS_GRANULARITY_1_HOUR = 1F

@Suppress("MagicNumber")
private fun LineChart.setDefaultSettings(chartData: LineChartData) {
    // General Chart Settings
    description.isEnabled = false
    extraBottomOffset = CHART_BOTTOM_OFFSET
    legend.isEnabled = false
    legend.textColor = resources.getColor(R.color.colorOnSurface, context.theme)

    // Line and highlight Settings
    lineData.setDrawValues(false)
    // General Chart Settings

    // Y Axis settings
    axisLeft.isGranularityEnabled = true
    isScaleYEnabled = false
    axisRight.isEnabled = false
    axisLeft.axisLineColor = resources.getColor(R.color.chart_axis_line_color, context.theme)
    axisLeft.gridColor = resources.getColor(R.color.colorBackground, context.theme)
    axisLeft.textColor = resources.getColor(R.color.colorOnSurface, context.theme)
    axisLeft.setLabelCount(MAXIMUMS_GRID_LINES_Y_AXIS, false)
    axisLeft.resetAxisMinimum()
    axisLeft.resetAxisMaximum()
    axisLeft.valueFormatter = CustomYAxisFormatter(chartData.unit)

    // X axis settings
    xAxis.position = XAxis.XAxisPosition.BOTTOM
    xAxis.setDrawAxisLine(false)
    xAxis.granularity = if (chartData.entries.size in 2..3) {
        X_AXIS_GRANULARITY_1_HOUR
    } else {
        X_AXIS_DEFAULT_TIME_GRANULARITY
    }
    xAxis.axisLineColor = resources.getColor(R.color.chart_axis_line_color, context.theme)
    xAxis.gridColor = resources.getColor(R.color.colorBackground, context.theme)
    xAxis.textColor = resources.getColor(R.color.colorOnSurface, context.theme)

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
    highLightColor = resources.getColor(R.color.colorOnSurface, context.theme)
    color = resources.getColor(R.color.colorPrimary, context.theme)
    setCircleColor(resources.getColor(R.color.colorPrimary, context.theme))
}

fun LineChart.initializeTemperature24hChart(chartData: LineChartData) {
    val dataSet = LineDataSet(chartData.entries, chartData.name)
    val lineData = LineData(dataSet)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings(chartData)

    // General Chart Settings
    marker =
        CustomDefaultMarkerView(context, chartData.timestamps, chartData.name, chartData.unit, 1)

    // Line and highlight Settings
    dataSet.setDefaultSettings(context, resources)

    // Y Axis settings

    /*
    * If max - min < 2 that means that the values are probably too close together.
    * Which causes a bug not showing labels on Y axis because granularity is set 1.
    * So this is a custom fix to add custom minimum and maximum values on the Y Axis
    */
    if (dataSet.yMax - dataSet.yMin < 2) {
        axisLeft.axisMinimum = dataSet.yMin - 1
        axisLeft.axisMaximum = dataSet.yMax + 1
    }

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(chartData.timestamps)
    show()
    notifyDataSetChanged()
}

fun LineChart.initializeHumidity24hChart(chartData: LineChartData) {
    val dataSet = LineDataSet(chartData.entries, chartData.name)
    val lineData = LineData(dataSet)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings(chartData)

    // General Chart Settings
    marker =
        CustomDefaultMarkerView(context, chartData.timestamps, chartData.name, chartData.unit)

    // Line and highlight Settings
    dataSet.setDefaultSettings(context, resources)

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(chartData.timestamps)
    show()
    notifyDataSetChanged()
}

@Suppress("MagicNumber")
fun LineChart.initializePressure24hChart(chartData: LineChartData) {
    val dataSet = LineDataSet(chartData.entries, chartData.name)
    val lineData = LineData(dataSet)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings(chartData)

    marker =
        CustomDefaultMarkerView(
            context,
            chartData.timestamps,
            chartData.name,
            chartData.unit,
            decimals = Weather.getDecimalsPressure()
        )

    // Line and highlight Settings
    dataSet.setDefaultSettings(context, resources)

    // Y Axis settings

    /*
    * If max - min < 2 that means that the values are probably too close together.
    * Which causes a bug not showing labels on Y axis because granularity is set 1.
    * So this is a custom fix to add custom minimum and maximum values on the Y Axis
    */
    if (dataSet.yMax - dataSet.yMin < 2) {
        if (chartData.unit == resources.getString(R.string.pressure_inHg)) {
            axisLeft.axisMinimum = dataSet.yMin - 0.1F
            axisLeft.axisMaximum = dataSet.yMax + 0.1F
            axisLeft.granularity = Y_AXIS_PRESSURE_INHG_GRANULARITY
            axisLeft.valueFormatter = CustomYAxisFormatter(chartData.unit, 2)
        } else {
            axisLeft.axisMinimum = dataSet.yMin - 1
            axisLeft.axisMaximum = dataSet.yMax + 1
        }
    }

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(chartData.timestamps)
    show()
    notifyDataSetChanged()
}

@Suppress("MagicNumber")
fun LineChart.initializePrecipitation24hChart(chartData: LineChartData) {
    val dataSet = LineDataSet(chartData.entries, chartData.name)
    dataSet.axisDependency = YAxis.AxisDependency.LEFT

    // use ILineDataSet to have multiple lines in a chart (in case we have probability)
    val dataSets = mutableListOf<ILineDataSet>()

    dataSets.add(dataSet)

    val lineData = LineData(dataSets)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings(chartData)

    val inchesUsed = chartData.unit == resources.getString(R.string.precipitation_in)

    marker = CustomDefaultMarkerView(
        context,
        chartData.timestamps,
        chartData.name,
        chartData.unit,
        decimals = Weather.getDecimalsPrecipitation()
    )

    // Precipitation Intensity Settings
    dataSet.setDefaultSettings(context, resources)
    dataSet.mode = LineDataSet.Mode.STEPPED
    dataSet.setDrawFilled(true)

    // Y Axis settings
    axisLeft.granularity = if (inchesUsed) {
        Y_AXIS_PRECIP_INCHES_GRANULARITY
    } else {
        Y_AXIS_1_DECIMAL_GRANULARITY
    }

    /*
    * If max - min < 0.1 that means that the values are probably too close together.
    * Which causes a bug not showing labels on Y axis or hiding the precip line behind the
    * X axis line.
    * That's why we set custom minimum and maximum values.
    */
    val customNumberForMinMax = if (inchesUsed) 0.01F else 0.1F
    if (dataSet.yMax - dataSet.yMin < customNumberForMinMax) {
        if (dataSet.yMin < customNumberForMinMax) {
            axisLeft.axisMinimum = 0F
            axisLeft.axisMaximum = dataSet.yMax + customNumberForMinMax
        } else {
            axisLeft.axisMinimum = dataSet.yMin - customNumberForMinMax
            axisLeft.axisMaximum = dataSet.yMax + customNumberForMinMax
        }
    } else {
        axisLeft.axisMinimum = dataSet.yMin
    }

    axisLeft.valueFormatter =
        CustomYAxisFormatter(chartData.unit, Weather.getDecimalsPrecipitation())

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(chartData.timestamps)
    show()
    notifyDataSetChanged()
}

@Suppress("MagicNumber")
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
    setDefaultSettings(windSpeedData)

    // General Chart Settings
    legend.isEnabled = true
    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
    marker = CustomWindMarkerView(
        context,
        windSpeedData.timestamps,
        windGustData.entries,
        windDirectionData.entries,
        windSpeedData.name,
        windGustData.name
    )

    // Wind Speed
    dataSetWindSpeed.setDefaultSettings(context, resources)

    // Wind Gust Settings
    dataSetWindGust.setDefaultSettings(context, resources)
    dataSetWindGust.setDrawIcons(false)
    dataSetWindGust.setDrawCircles(false)
    dataSetWindGust.enableDashedLine(30.0F, 20.0F, 0.0F)
    dataSetWindGust.isHighlightEnabled = false
    dataSetWindGust.color = resources.getColor(R.color.chart_wind_gust_color, context.theme)

    // Y Axis settings
    /*
    * If max - min < 2 that means that the values are probably too close together.
    * Which causes a bug not showing labels on Y axis because granularity is set 1.
    * So this is a custom fix to add custom minimum and maximum values on the Y Axis
    * NOTE: Wind Gust is always equal or higher than wind speed that's why we use its max
    */
    if (dataSetWindGust.yMax - dataSetWindSpeed.yMin < 2) {
        if (dataSetWindSpeed.yMin < 1) {
            axisLeft.axisMinimum = 0F
            axisLeft.axisMaximum = dataSetWindGust.yMax + 2
        } else {
            axisLeft.axisMinimum = dataSetWindSpeed.yMin - 1
            axisLeft.axisMaximum = dataSetWindGust.yMax + 1
        }
    }
    axisLeft.valueFormatter = CustomYAxisFormatter(windSpeedData.unit)

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(windGustData.timestamps)
    show()
    notifyDataSetChanged()
}

@Suppress("MagicNumber")
fun BarChart.initializeUV24hChart(data: BarChartData) {
    val dataSet = BarDataSet(data.entries, data.name)
    val barData = BarData(dataSet)
    setData(barData)

    // General Chart Settings
    description.isEnabled = false
    legend.isEnabled = false
    legend.textColor = resources.getColor(R.color.colorOnSurface, context.theme)
    marker = CustomDefaultMarkerView(context, data.timestamps, data.name, data.unit)
    extraBottomOffset = CHART_BOTTOM_OFFSET

    // Bar and highlight Settings
    barData.setDrawValues(false)
    dataSet.color = resources.getColor(R.color.colorPrimary, context.theme)
    dataSet.highLightColor = resources.getColor(R.color.colorPrimaryVariant, context.theme)

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
    axisLeft.valueFormatter = CustomYAxisFormatter(data.unit)
    axisLeft.textColor = resources.getColor(R.color.colorOnSurface, context.theme)
    axisLeft.axisLineColor = resources.getColor(R.color.chart_axis_line_color, context.theme)
    axisLeft.gridColor = resources.getColor(R.color.colorBackground, context.theme)
    axisRight.isEnabled = false
    isScaleYEnabled = false

    // X axis settings
    xAxis.position = XAxis.XAxisPosition.BOTTOM
    xAxis.setDrawAxisLine(false)
    xAxis.setDrawGridLines(false)
    xAxis.valueFormatter = CustomXAxisFormatter(data.timestamps)
    xAxis.textColor = resources.getColor(R.color.colorOnSurface, context.theme)
    xAxis.granularity = if (data.entries.size in 2..3) {
        X_AXIS_GRANULARITY_1_HOUR
    } else {
        X_AXIS_DEFAULT_TIME_GRANULARITY
    }
    xAxis.axisLineColor = resources.getColor(R.color.chart_axis_line_color, context.theme)
    xAxis.gridColor = resources.getColor(R.color.colorBackground, context.theme)
    show()
    notifyDataSetChanged()
}
