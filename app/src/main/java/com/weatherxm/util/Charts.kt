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
    with(axisLeft) {
        axisLineColor = resources.getColor(R.color.chart_axis_line_color, context.theme)
        gridColor = resources.getColor(R.color.colorBackground, context.theme)
        textColor = resources.getColor(R.color.colorOnSurface, context.theme)
        setLabelCount(MAXIMUMS_GRID_LINES_Y_AXIS, false)
        resetAxisMinimum()
        resetAxisMaximum()
        valueFormatter = CustomYAxisFormatter(chartData.unit)
    }


    // X axis settings
    with(xAxis) {
        position = XAxis.XAxisPosition.BOTTOM
        setDrawAxisLine(false)
        granularity = if (chartData.entries.size in 2..3) {
            X_AXIS_GRANULARITY_1_HOUR
        } else {
            X_AXIS_DEFAULT_TIME_GRANULARITY
        }
        axisLineColor = resources.getColor(R.color.chart_axis_line_color, context.theme)
        gridColor = resources.getColor(R.color.colorBackground, context.theme)
        textColor = resources.getColor(R.color.colorOnSurface, context.theme)
    }

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
    color = resources.getColor(R.color.chart_primary_line, context.theme)
    setCircleColor(resources.getColor(R.color.chart_primary_line, context.theme))
}

@Suppress("MagicNumber")
fun LineChart.initializeTemperature24hChart(
    temperatureData: LineChartData,
    feelsLikeData: LineChartData
) {
    val dataSetTemperature = LineDataSet(temperatureData.entries, temperatureData.name)
    val dataSetFeelsLike = LineDataSet(feelsLikeData.entries, feelsLikeData.name)

    dataSetTemperature.axisDependency = YAxis.AxisDependency.LEFT

    // use the interface ILineDataSet to have multiple lines in a chart
    val dataSets = mutableListOf<ILineDataSet>()
    dataSets.add(dataSetFeelsLike)
    dataSets.add(dataSetTemperature)
    val lineData = LineData(dataSets)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings(temperatureData)

    // General Chart Settings
    legend.isEnabled = true
    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
    marker = CustomTemperatureMarkerView(
        context,
        temperatureData.timestamps,
        feelsLikeData.entries,
        temperatureData.name,
        feelsLikeData.name
    )

    // Temperature Settings
    dataSetTemperature.setDefaultSettings(context, resources)

    // Feels Like Settings
    with(dataSetFeelsLike) {
        setDefaultSettings(context, resources)
        setDrawCircles(false)
        color = resources.getColor(R.color.chart_secondary_line, context.theme)
        isHighlightEnabled = false
    }

    // Y Axis settings

    /*
    * If max - min < 2 that means that the values are probably too close together.
    * Which causes a bug not showing labels on Y axis because granularity is set 1.
    * So this is a custom fix to add custom minimum and maximum values on the Y Axis
    */
    with(axisLeft) {
        if (dataSetTemperature.yMax > dataSetFeelsLike.yMax) {
            // If we get here that means feels like is lower than temperature this day
            if (dataSetTemperature.yMax - dataSetFeelsLike.yMin < 2) {
                axisMinimum = dataSetFeelsLike.yMin - 1
                axisMaximum = dataSetTemperature.yMax + 1
            }
        } else {
            // If we get here that means feels like is higher than temperature this day
            if (dataSetFeelsLike.yMax - dataSetTemperature.yMin < 2) {
                axisMinimum = dataSetTemperature.yMin - 1
                axisMaximum = dataSetFeelsLike.yMax + 1
            }
        }
    }

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(temperatureData.timestamps)
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
    with(axisLeft) {
        if (dataSet.yMax - dataSet.yMin < 2) {
            if (chartData.unit == resources.getString(R.string.pressure_inHg)) {
                axisMinimum = dataSet.yMin - 0.1F
                axisMaximum = dataSet.yMax + 0.1F
                granularity = Y_AXIS_PRESSURE_INHG_GRANULARITY
                valueFormatter = CustomYAxisFormatter(chartData.unit, 2)
            } else {
                axisMinimum = dataSet.yMin - 1
                axisMaximum = dataSet.yMax + 1
            }
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
    with(axisLeft) {
        granularity = if (inchesUsed) {
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
                axisMinimum = 0F
                axisMaximum = dataSet.yMax + customNumberForMinMax
            } else {
                axisMinimum = dataSet.yMin - customNumberForMinMax
                axisMaximum = dataSet.yMax + customNumberForMinMax
            }
        } else {
            axisMinimum = dataSet.yMin
        }

        valueFormatter = CustomYAxisFormatter(chartData.unit, Weather.getDecimalsPrecipitation())
    }

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
    dataSets.add(dataSetWindGust)
    dataSets.add(dataSetWindSpeed)
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
    with(dataSetWindGust) {
        setDefaultSettings(context, resources)
        setDrawCircles(false)
        color = resources.getColor(R.color.chart_secondary_line, context.theme)
        isHighlightEnabled = false
    }

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
    with(axisLeft) {
        axisMinimum = 0F
        isGranularityEnabled = true
        granularity = 1F
        valueFormatter = CustomYAxisFormatter(data.unit)
        textColor = resources.getColor(R.color.colorOnSurface, context.theme)
        axisLineColor = resources.getColor(R.color.chart_axis_line_color, context.theme)
        gridColor = resources.getColor(R.color.colorBackground, context.theme)
    }
    axisRight.isEnabled = false
    isScaleYEnabled = false

    // X axis settings
    with(xAxis) {
        position = XAxis.XAxisPosition.BOTTOM
        setDrawAxisLine(false)
        setDrawGridLines(false)
        valueFormatter = CustomXAxisFormatter(data.timestamps)
        textColor = resources.getColor(R.color.colorOnSurface, context.theme)
        granularity = if (data.entries.size in 2..3) {
            X_AXIS_GRANULARITY_1_HOUR
        } else {
            X_AXIS_DEFAULT_TIME_GRANULARITY
        }
        axisLineColor = resources.getColor(R.color.chart_axis_line_color, context.theme)
        gridColor = resources.getColor(R.color.colorBackground, context.theme)
    }
    show()
    notifyDataSetChanged()
}
