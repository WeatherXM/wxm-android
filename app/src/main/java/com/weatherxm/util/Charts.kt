package com.weatherxm.util

import android.content.Context
import android.content.res.Resources
import android.view.MotionEvent
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.weatherxm.R
import com.weatherxm.ui.common.show
import com.weatherxm.ui.devicehistory.BarChartData
import com.weatherxm.ui.devicehistory.LineChartData

private const val CHART_BOTTOM_OFFSET = 20F
private const val LINE_WIDTH = 2F
private const val POINT_SIZE = 2F
private const val MAXIMUMS_GRID_LINES_Y_AXIS = 4
private const val Y_AXIS_1_DECIMAL_GRANULARITY = 0.1F
private const val Y_AXIS_PRECIP_INCHES_GRANULARITY = 0.01F
private const val Y_AXIS_PRESSURE_INHG_GRANULARITY = 0.01F
private const val X_AXIS_DEFAULT_TIME_GRANULARITY = 3F

@Suppress("MagicNumber")
private fun LineChart.setDefaultSettings(chartData: LineChartData) {
    // General Chart Settings
    description.isEnabled = false
    extraBottomOffset = CHART_BOTTOM_OFFSET
    legend.isEnabled = false
    legend.textColor = resources.getColor(R.color.colorOnSurface, context.theme)

    // Line and highlight Settings
    lineData.setDrawValues(false)

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
        granularity = X_AXIS_DEFAULT_TIME_GRANULARITY
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

private fun MutableList<LineDataSet>.secondaryLineInit(
    context: Context,
    resources: Resources
): MutableList<LineDataSet> {
    forEach {
        with(it) {
            setDefaultSettings(context, resources)
            isHighlightEnabled = false
            color = resources.getColor(R.color.chart_secondary_line, context.theme)
            setCircleColor(resources.getColor(R.color.chart_secondary_line, context.theme))
        }
    }
    return this
}

private fun MutableList<LineDataSet>.primaryLineInit(
    context: Context,
    resources: Resources
): MutableList<LineDataSet> {
    forEach {
        it.setDefaultSettings(context, resources)
    }
    return this
}

@Suppress("MagicNumber")
private fun newLegendEntry(label: String, formLineWidth: Float, color: Int): LegendEntry {
    return LegendEntry(label, Legend.LegendForm.CIRCLE, 10F, formLineWidth, null, color)
}

fun LineChart.initializeTemperature24hChart(
    temperatureData: LineChartData,
    feelsLikeData: LineChartData
) {
    val temperatureLineDataSetsWithValues = temperatureData.getLineDataSetsWithValues()
    val temperatureEmptyLineDataSets = temperatureData.getEmptyLineDataSets()
    val feelsLikeLineDataSetsWithValues = feelsLikeData.getLineDataSetsWithValues()
    val feelsLikeEmptyLineDataSets = feelsLikeData.getEmptyLineDataSets()
    val dataSets = mutableListOf<ILineDataSet>()

    dataSets.addAll(feelsLikeLineDataSetsWithValues.secondaryLineInit(context, resources))
    dataSets.addAll(feelsLikeEmptyLineDataSets)
    dataSets.addAll(temperatureLineDataSetsWithValues.primaryLineInit(context, resources))
    dataSets.addAll(temperatureEmptyLineDataSets)

    val lineData = LineData(dataSets)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings(temperatureData)

    // General Chart Settings
    legend.isEnabled = true
    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
    val legendEntries = mutableListOf(
        newLegendEntry(
            temperatureData.name,
            legend.formLineWidth,
            resources.getColor(R.color.chart_primary_line, context.theme)
        ),
        newLegendEntry(
            feelsLikeData.name,
            legend.formLineWidth,
            resources.getColor(R.color.chart_secondary_line, context.theme)
        )
    )
    legend.setCustom(legendEntries)
    marker = CustomTemperatureMarkerView(
        context,
        temperatureData.timestamps,
        feelsLikeData.entries,
        temperatureData.name,
        feelsLikeData.name
    )

    // Y Axis settings

    /*
    * If max - min < 2 that means that the values are probably too close together.
    * Which causes a bug not showing labels on Y axis because granularity is set 1.
    * So this is a custom fix to add custom minimum and maximum values on the Y Axis
    */
    val yMinTemperature = temperatureLineDataSetsWithValues.minOf { it.yMin }
    val yMaxTemperature = temperatureLineDataSetsWithValues.maxOf { it.yMax }
    val yMinFeelsLike = feelsLikeLineDataSetsWithValues.minOf { it.yMin }
    val yMaxFeelsLike = feelsLikeLineDataSetsWithValues.maxOf { it.yMax }
    with(axisLeft) {
        if (yMaxTemperature > yMaxFeelsLike) {
            // If we get here that means feels like is lower than temperature this day
            if (yMaxTemperature - yMinFeelsLike < 2) {
                axisMinimum = yMinFeelsLike - 1
                axisMaximum = yMaxTemperature + 1
            }
        } else {
            // If we get here that means feels like is higher than temperature this day
            if (yMaxFeelsLike - yMinTemperature < 2) {
                axisMinimum = yMinTemperature - 1
                axisMaximum = yMaxFeelsLike + 1
            }
        }
    }

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(temperatureData.timestamps)
    show()
    notifyDataSetChanged()
}

fun LineChart.initializeHumidity24hChart(chartData: LineChartData) {
    val lineDataSetsWithValues = chartData.getLineDataSetsWithValues()
    val emptyLineDataSets = chartData.getEmptyLineDataSets()
    val dataSets = mutableListOf<ILineDataSet>()

    dataSets.addAll(lineDataSetsWithValues.primaryLineInit(context, resources))
    dataSets.addAll(emptyLineDataSets)
    val lineData = LineData(dataSets)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings(chartData)

    // General Chart Settings
    marker = CustomDefaultMarkerView(context, chartData.timestamps, chartData.name, chartData.unit)

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(chartData.timestamps)
    show()
    notifyDataSetChanged()
}

@Suppress("MagicNumber")
fun LineChart.initializePressure24hChart(chartData: LineChartData) {
    val lineDataSetsWithValues = chartData.getLineDataSetsWithValues()
    val emptyLineDataSets = chartData.getEmptyLineDataSets()
    val dataSets = mutableListOf<ILineDataSet>()

    dataSets.addAll(lineDataSetsWithValues.primaryLineInit(context, resources))
    dataSets.addAll(emptyLineDataSets)

    val lineData = LineData(dataSets)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings(chartData)

    marker = CustomDefaultMarkerView(
        context,
        chartData.timestamps,
        chartData.name,
        chartData.unit,
        Weather.getDecimalsPressure()
    )

    // Y Axis settings

    /*
    * If max - min < 2 that means that the values are probably too close together.
    * Which causes a bug not showing labels on Y axis because granularity is set 1.
    * So this is a custom fix to add custom minimum and maximum values on the Y Axis
    */
    with(axisLeft) {
        val yMin = lineDataSetsWithValues.minOf { it.yMin }
        val yMax = lineDataSetsWithValues.maxOf { it.yMax }
        if (yMax - yMin < 2) {
            if (chartData.unit == resources.getString(R.string.pressure_inHg)) {
                axisMinimum = yMin - 0.1F
                axisMaximum = yMax + 0.1F
                granularity = Y_AXIS_PRESSURE_INHG_GRANULARITY
                valueFormatter = CustomYAxisFormatter(chartData.unit, 2)
            } else {
                axisMinimum = yMin - 1
                axisMaximum = yMax + 1
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
    val lineDataSetsWithValues = chartData.getLineDataSetsWithValues()
    val emptyLineDataSets = chartData.getEmptyLineDataSets()
    val dataSets = mutableListOf<ILineDataSet>()

    dataSets.addAll(lineDataSetsWithValues.primaryLineInit(context, resources))
    lineDataSetsWithValues.forEach {
        // Precipitation Intensity Settings
        it.mode = LineDataSet.Mode.STEPPED
        it.setDrawFilled(true)
    }
    dataSets.addAll(emptyLineDataSets)
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
        Weather.getDecimalsPrecipitation()
    )

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
        val yMin = lineDataSetsWithValues.minOf { it.yMin }
        val yMax = lineDataSetsWithValues.maxOf { it.yMax }
        if (yMax - yMin < customNumberForMinMax) {
            if (yMin < customNumberForMinMax) {
                axisMinimum = 0F
                axisMaximum = yMax + customNumberForMinMax
            } else {
                axisMinimum = yMin - customNumberForMinMax
                axisMaximum = yMax + customNumberForMinMax
            }
        } else {
            axisMinimum = yMin
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
    val windSpeedLineDataSetsWithValues = windSpeedData.getLineDataSetsWithValues()
    val windSpeedEmptyLineDataSets = windSpeedData.getEmptyLineDataSets()
    val windGustLineDataSetsWithValues = windGustData.getLineDataSetsWithValues()
    val windGustEmptyLineDataSets = windGustData.getEmptyLineDataSets()
    val dataSets = mutableListOf<ILineDataSet>()

    dataSets.addAll(windGustLineDataSetsWithValues.secondaryLineInit(context, resources))
    dataSets.addAll(windGustEmptyLineDataSets)
    dataSets.addAll(windSpeedLineDataSetsWithValues.primaryLineInit(context, resources))
    dataSets.addAll(windSpeedEmptyLineDataSets)

    val lineData = LineData(dataSets)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings(windSpeedData)

    // General Chart Settings
    legend.isEnabled = true
    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
    val legendEntries = mutableListOf(
        newLegendEntry(
            windSpeedData.name,
            legend.formLineWidth,
            resources.getColor(R.color.chart_primary_line, context.theme)
        ),
        newLegendEntry(
            windGustData.name,
            legend.formLineWidth,
            resources.getColor(R.color.chart_secondary_line, context.theme)
        )
    )
    legend.setCustom(legendEntries)
    marker = CustomWindMarkerView(
        context,
        windSpeedData.timestamps,
        windGustData.entries,
        windDirectionData.entries,
        windSpeedData.name,
        windGustData.name
    )

    // Y Axis settings
    /*
    * If max - min < 2 that means that the values are probably too close together.
    * Which causes a bug not showing labels on Y axis because granularity is set 1.
    * So this is a custom fix to add custom minimum and maximum values on the Y Axis
    * NOTE: Wind Gust is always equal or higher than wind speed that's why we use its max
    */
    val yMin = windSpeedLineDataSetsWithValues.minOf { it.yMin }
    val yMax = windGustLineDataSetsWithValues.maxOf { it.yMax }
    if (yMax - yMin < 2) {
        if (yMin < 1) {
            axisLeft.axisMinimum = 0F
            axisLeft.axisMaximum = yMax + 2
        } else {
            axisLeft.axisMinimum = yMin - 1
            axisLeft.axisMaximum = yMax + 1
        }
    }
    axisLeft.valueFormatter = CustomYAxisFormatter(windSpeedData.unit)

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(windGustData.timestamps)
    show()
    notifyDataSetChanged()
}

@Suppress("MagicNumber")
fun BarChart.initializeUV24hChart(chartData: BarChartData) {
    val dataSet = BarDataSet(chartData.entries, chartData.name)
    val barData = BarData(dataSet)
    setData(barData)

    // General Chart Settings
    description.isEnabled = false
    legend.isEnabled = false
    legend.textColor = resources.getColor(R.color.colorOnSurface, context.theme)
    marker = CustomDefaultMarkerView(context, chartData.timestamps, chartData.name, chartData.unit)
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
        valueFormatter = CustomYAxisFormatter(chartData.unit)
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
        valueFormatter = CustomXAxisFormatter(chartData.timestamps)
        textColor = resources.getColor(R.color.colorOnSurface, context.theme)
        granularity = X_AXIS_DEFAULT_TIME_GRANULARITY
        axisLineColor = resources.getColor(R.color.chart_axis_line_color, context.theme)
        gridColor = resources.getColor(R.color.colorBackground, context.theme)
    }
    show()
    notifyDataSetChanged()
}
