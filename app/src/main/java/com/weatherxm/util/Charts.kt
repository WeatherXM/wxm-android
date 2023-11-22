@file:Suppress("TooManyFunctions")

package com.weatherxm.util

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.view.MotionEvent
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.weatherxm.R
import com.weatherxm.ui.common.show
import com.weatherxm.ui.devicehistory.LineChartData
import com.weatherxm.util.Weather.getDecimalsPressure
import com.weatherxm.util.Weather.roundToDecimals
import com.weatherxm.util.Weather.roundToInt

private const val CHART_OFFSET = 5F
private const val LINE_WIDTH = 2F
private const val POINT_SIZE = 2F
private const val MAXIMUM_GRID_LINES_Y_AXIS = 4
private const val Y_AXIS_1_DECIMAL_GRANULARITY = 0.1F
private const val Y_AXIS_PRECIP_INCHES_GRANULARITY = 0.01F
private const val Y_AXIS_PRESSURE_INHG_GRANULARITY = 0.01F
private const val Y_AXIS_LABEL_LENGTH = 4
private const val X_AXIS_DEFAULT_TIME_GRANULARITY = 3F

@Suppress("MagicNumber")
private fun LineChart.setDefaultSettings(context: Context) {
    // General Chart Settings
    description.isEnabled = false
    extraTopOffset = CHART_OFFSET
    extraBottomOffset = CHART_OFFSET
    extraLeftOffset = CHART_OFFSET
    extraRightOffset = CHART_OFFSET
    legend.isEnabled = false
    legend.textColor = resources.getColor(R.color.colorOnSurface, context.theme)

    // Line and highlight Settings
    lineData.setDrawValues(false)

    // Y Axis settings
    isScaleYEnabled = false
    axisLeft.setDefaultSettings(context)
    axisRight.setDefaultSettings(context, isAxisLeft = false)
    axisLeft.isEnabled = true
    axisRight.isEnabled = false

    // X axis settings
    with(xAxis) {
        typeface = Typeface.MONOSPACE
        position = XAxis.XAxisPosition.BOTTOM
        setDrawAxisLine(false)
        granularity = X_AXIS_DEFAULT_TIME_GRANULARITY
        axisLineColor = resources.getColor(R.color.colorOnSurfaceVariant, context.theme)
        gridColor = resources.getColor(R.color.colorBackground, context.theme)
        textColor = resources.getColor(R.color.colorOnSurface, context.theme)
    }

    handleGestures()
}

private fun LineChart.handleGestures() {
    onChartGestureListener = object : OnChartGestureListener {
        override fun onChartGestureStart(
            me: MotionEvent?,
            lastPerformedGesture: ChartTouchListener.ChartGesture?
        ) {
            parent.requestDisallowInterceptTouchEvent(true)
        }

        override fun onChartGestureEnd(
            me: MotionEvent?,
            lastPerformedGesture: ChartTouchListener.ChartGesture?
        ) {
            parent.requestDisallowInterceptTouchEvent(false)
        }

        override fun onChartLongPressed(me: MotionEvent?) {
            // Do nothing
        }

        override fun onChartDoubleTapped(me: MotionEvent?) {
            // Do nothing
        }

        override fun onChartSingleTapped(me: MotionEvent?) {
            // Do nothing
        }

        override fun onChartFling(
            me1: MotionEvent?,
            me2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ) {
            // Do nothing
        }

        override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {
            // Do nothing
        }

        override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {
            // Do nothing
        }
    }
}

@Suppress("MagicNumber")
private fun LineDataSet.setDefaultSettings(context: Context, resources: Resources) {
    setDrawCircleHole(false)
    circleRadius = POINT_SIZE
    lineWidth = LINE_WIDTH
    mode = LineDataSet.Mode.CUBIC_BEZIER
    highLightColor = context.getColor(R.color.colorPrimary)
    setDrawHorizontalHighlightIndicator(false)
    enableDashedHighlightLine(10F, 4F, 0F)
    color = resources.getColor(R.color.colorPrimary, context.theme)
    setCircleColor(resources.getColor(R.color.colorPrimary, context.theme))
}

private fun YAxis.setDefaultSettings(context: Context, isAxisLeft: Boolean = true) {
    typeface = Typeface.MONOSPACE
    isGranularityEnabled = true
    resetAxisMinimum()
    resetAxisMaximum()
    valueFormatter = CustomYAxisFormatter(isAxisLeft = isAxisLeft)
    setLabelCount(MAXIMUM_GRID_LINES_Y_AXIS, false)
    axisLineColor = context.getColor(R.color.colorOnSurfaceVariant)
    gridColor = context.getColor(R.color.colorBackground)
    textColor = context.getColor(R.color.colorOnSurface)

    if (!isAxisLeft) {
        setDrawGridLines(false)
    }
}

private fun MutableList<LineDataSet>.secondaryLineInit(
    context: Context, resources: Resources
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
    context: Context, resources: Resources
): MutableList<LineDataSet> {
    forEach {
        it.setDefaultSettings(context, resources)
    }
    return this
}

private fun createDataSetToXPointsMap(
    initialSize: Int,
    dataSet: MutableList<LineDataSet>
): MutableMap<Int, List<Float>> {
    val dataSetIndexToXPoints = mutableMapOf<Int, List<Float>>()
    var dataSetIndex = initialSize
    dataSet.forEach {
        val xPoints = it.values.map { entry ->
            entry.x
        }
        dataSetIndexToXPoints[dataSetIndex] = xPoints
        dataSetIndex++
    }
    return dataSetIndexToXPoints
}

fun LineChart.initTemperature24hChart(
    temperatureData: LineChartData, feelsLikeData: LineChartData
): MutableMap<Int, List<Float>> {
    val tempLabel = resources.getString(R.string.temperature)
    val feelsLikeLabel = resources.getString(R.string.feels_like)

    val temperatureLineDataSetsWithValues = temperatureData.getLineDataSetsWithValues(tempLabel)
    val temperatureEmptyLineDataSets = temperatureData.getEmptyLineDataSets(tempLabel)
    val feelsLikeLineDataSetsWithValues = feelsLikeData.getLineDataSetsWithValues(feelsLikeLabel)
    val feelsLikeEmptyLineDataSets = feelsLikeData.getEmptyLineDataSets(feelsLikeLabel)
    val dataSets = mutableListOf<ILineDataSet>()

    dataSets.addAll(feelsLikeLineDataSetsWithValues.secondaryLineInit(context, resources))
    dataSets.addAll(feelsLikeEmptyLineDataSets)
    dataSets.addAll(temperatureLineDataSetsWithValues.primaryLineInit(context, resources))
    dataSets.addAll(temperatureEmptyLineDataSets)

    val lineData = LineData(dataSets)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings(context)

    /**
     * Y Axis settings
     *
     * If max - min < 2 that means that the values are probably too close together.
     * Which causes a bug not showing labels on Y axis because granularity is set 1.
     * So this is a custom fix to add custom minimum and maximum values on the Y Axis
     */
    val yMinTemperature = temperatureLineDataSetsWithValues.minOf { it.yMin }
    val yMaxTemperature = temperatureLineDataSetsWithValues.maxOf { it.yMax }
    val yMinFeelsLike = feelsLikeLineDataSetsWithValues.minOf { it.yMin }
    val yMaxFeelsLike = feelsLikeLineDataSetsWithValues.maxOf { it.yMax }
    axisLeft.axisMinimum = if (yMinFeelsLike < yMinTemperature) {
        yMinFeelsLike - 1
    } else {
        yMinTemperature - 1
    }
    axisLeft.axisMaximum = if (yMaxFeelsLike > yMaxTemperature) {
        yMaxFeelsLike + 1
    } else {
        yMaxTemperature + 1
    }

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(temperatureData.timestamps)
    show()
    notifyDataSetChanged()

    return createDataSetToXPointsMap(
        feelsLikeLineDataSetsWithValues.size + feelsLikeEmptyLineDataSets.size,
        temperatureLineDataSetsWithValues
    )
}

fun LineChart.initHumidity24hChart(chartData: LineChartData): MutableMap<Int, List<Float>> {
    val label = resources.getString(R.string.humidity)

    val lineDataSetsWithValues = chartData.getLineDataSetsWithValues(label)
    val emptyLineDataSets = chartData.getEmptyLineDataSets(label)
    val dataSets = mutableListOf<ILineDataSet>()

    dataSets.addAll(lineDataSetsWithValues.primaryLineInit(context, resources))
    dataSets.addAll(emptyLineDataSets)
    val lineData = LineData(dataSets)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings(context)

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(chartData.timestamps)
    show()
    notifyDataSetChanged()

    return createDataSetToXPointsMap(0, lineDataSetsWithValues)
}

@Suppress("MagicNumber")
fun LineChart.initPressure24hChart(chartData: LineChartData): MutableMap<Int, List<Float>> {
    val label = resources.getString(R.string.pressure)
    val lineDataSetsWithValues = chartData.getLineDataSetsWithValues(label)
    val emptyLineDataSets = chartData.getEmptyLineDataSets(label)
    val dataSets = mutableListOf<ILineDataSet>()

    dataSets.addAll(lineDataSetsWithValues.primaryLineInit(context, resources))
    dataSets.addAll(emptyLineDataSets)

    val lineData = LineData(dataSets)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings(context)

    /**
     * Y Axis settings
     *
     * If max - min < 2 that means that the values are probably too close together.
     * Which causes a bug not showing labels on Y axis because granularity is set 1.
     * So this is a custom fix to add custom minimum and maximum values on the Y Axis
     */
    val yMin = lineDataSetsWithValues.minOf { it.yMin }
    val yMax = lineDataSetsWithValues.maxOf { it.yMax }
    if (yMax - yMin < 2) {
        if (getDecimalsPressure() == 2) {
            axisLeft.axisMinimum = yMin - 0.1F
            axisLeft.axisMaximum = yMax + 0.1F
            axisLeft.granularity = Y_AXIS_PRESSURE_INHG_GRANULARITY
            axisLeft.valueFormatter = CustomYAxisFormatter(1)
        } else {
            axisLeft.axisMinimum = yMin - 1
            axisLeft.axisMaximum = yMax + 1
        }
    }

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(chartData.timestamps)
    show()
    notifyDataSetChanged()

    return createDataSetToXPointsMap(0, lineDataSetsWithValues)
}

@Suppress("MagicNumber", "LongMethod")
fun LineChart.initPrecipitation24hChart(
    rateData: LineChartData, accumulatedData: LineChartData
): MutableMap<Int, List<Float>> {
    val rateLabel = resources.getString(R.string.precipitation_rate)
    val accumulationLabel = resources.getString(R.string.daily_precipitation)

    val rateLineDataSetsWithValues = rateData.getLineDataSetsWithValues(rateLabel)
    val rateEmptyLineDataSets = rateData.getEmptyLineDataSets(rateLabel)
    val accumulatedDataLineDataSetsWithValues =
        accumulatedData.getLineDataSetsWithValues(accumulationLabel)
    val accumulatedDataEmptyLineDataSets = accumulatedData.getEmptyLineDataSets(accumulationLabel)
    val dataSets = mutableListOf<ILineDataSet>()

    dataSets.addAll(accumulatedDataLineDataSetsWithValues.secondaryLineInit(context, resources))
    accumulatedDataLineDataSetsWithValues.forEach {
        // Precipitation Accumulated Settings
        it.axisDependency = YAxis.AxisDependency.RIGHT
        if (it.values.size > 1) it.setDrawCircles(false)
        it.lineWidth = 0.2F
        it.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        it.setDrawFilled(true)
        it.color = resources.getColor(R.color.chart_secondary_line, context.theme)
        it.fillColor = context.getColor(R.color.chart_secondary_line)
    }
    dataSets.addAll(accumulatedDataEmptyLineDataSets)
    dataSets.addAll(rateLineDataSetsWithValues.primaryLineInit(context, resources))
    rateLineDataSetsWithValues.forEach {
        // Precipitation Intensity Settings
        if (it.values.size > 1) it.setDrawCircles(false)
        it.axisDependency = YAxis.AxisDependency.LEFT
        it.mode = LineDataSet.Mode.STEPPED
    }
    dataSets.addAll(rateEmptyLineDataSets)

    val lineData = LineData(dataSets)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings(context)

    // General Chart Settings
    val decimals = getDecimalsPressure()

    // Y Axis settings
    val customNumberForMinMax = if (decimals == 2) 0.01F else 0.1F
    with(axisLeft) {
        granularity = if (decimals == 2) {
            Y_AXIS_PRECIP_INCHES_GRANULARITY
        } else {
            Y_AXIS_1_DECIMAL_GRANULARITY
        }

        /**
         * If max - min < 0.1 that means that the values are probably too close together.
         * Which causes a bug not showing labels on Y axis or hiding the precip line behind the
         * X axis line.
         * That's why we set custom minimum and maximum values.
         */
        val yMin = rateLineDataSetsWithValues.minOf { it.yMin }
        val yMax = rateLineDataSetsWithValues.maxOf { it.yMax }
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

        valueFormatter = CustomYAxisFormatter(Weather.getDecimalsPrecipitation())
    }

    with(axisRight) {
        isEnabled = true
        valueFormatter = CustomYAxisFormatter(Weather.getDecimalsPrecipitation(), false)

        granularity = if (decimals == 2) {
            Y_AXIS_PRECIP_INCHES_GRANULARITY
        } else {
            Y_AXIS_1_DECIMAL_GRANULARITY
        }

        val yMin = accumulatedDataLineDataSetsWithValues.minOf { it.yMin }
        val yMax = accumulatedDataLineDataSetsWithValues.maxOf { it.yMax }
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
    }

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(rateData.timestamps)
    show()
    notifyDataSetChanged()

    return createDataSetToXPointsMap(
        rateLineDataSetsWithValues.size + rateEmptyLineDataSets.size,
        accumulatedDataLineDataSetsWithValues
    )
}

@Suppress("MagicNumber")
fun LineChart.initWind24hChart(
    windSpeedData: LineChartData,
    windGustData: LineChartData
): MutableMap<Int, List<Float>> {
    val speedLabel = resources.getString(R.string.wind_speed)
    val gustLabel = resources.getString(R.string.wind_gust)

    val windSpeedLineDataSetsWithValues = windSpeedData.getLineDataSetsWithValues(speedLabel)
    val windSpeedEmptyLineDataSets = windSpeedData.getEmptyLineDataSets(speedLabel)
    val windGustLineDataSetsWithValues = windGustData.getLineDataSetsWithValues(gustLabel)
    val windGustEmptyLineDataSets = windGustData.getEmptyLineDataSets(gustLabel)
    val dataSets = mutableListOf<ILineDataSet>()

    dataSets.addAll(windGustLineDataSetsWithValues.secondaryLineInit(context, resources))
    dataSets.addAll(windGustEmptyLineDataSets)
    dataSets.addAll(windSpeedLineDataSetsWithValues.primaryLineInit(context, resources))
    dataSets.addAll(windSpeedEmptyLineDataSets)

    val lineData = LineData(dataSets)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings(context)

    /**
     * Y AXIS SETTINGS
     *
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

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(windGustData.timestamps)
    show()
    notifyDataSetChanged()

    return createDataSetToXPointsMap(
        windGustLineDataSetsWithValues.size + windGustEmptyLineDataSets.size,
        windSpeedLineDataSetsWithValues
    )
}

@Suppress("MagicNumber")
fun LineChart.initSolarChart(
    uvData: LineChartData, radiationData: LineChartData
): MutableMap<Int, List<Float>> {
    val uvLabel = resources.getString(R.string.uv_index)
    val solarLabel = resources.getString(R.string.solar_radiation)

    val uvLineDataSetsWithValues = uvData.getLineDataSetsWithValues(uvLabel)
    val uvEmptyLineDataSets = uvData.getEmptyLineDataSets(uvLabel)
    val radiationDataLineDataSetsWithValues = radiationData.getLineDataSetsWithValues(solarLabel)
    val radiationDataEmptyLineDataSets = radiationData.getEmptyLineDataSets(solarLabel)
    val dataSets = mutableListOf<ILineDataSet>()

    dataSets.addAll(radiationDataLineDataSetsWithValues.secondaryLineInit(context, resources))
    radiationDataLineDataSetsWithValues.forEach {
        // Radiation Settings
        if (it.values.size > 1) it.setDrawCircles(false)
        it.setDrawFilled(true)
        it.lineWidth = 0.2F
        it.axisDependency = YAxis.AxisDependency.RIGHT
        it.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        it.color = resources.getColor(R.color.chart_secondary_line, context.theme)
        it.fillColor = context.getColor(R.color.chart_secondary_line)
    }
    dataSets.addAll(radiationDataEmptyLineDataSets)

    dataSets.addAll(uvLineDataSetsWithValues.primaryLineInit(context, resources))
    uvLineDataSetsWithValues.forEach {
        // UV Settings
        it.axisDependency = YAxis.AxisDependency.LEFT
        if (it.values.size > 1) it.setDrawCircles(false)
        it.mode = LineDataSet.Mode.STEPPED
    }
    dataSets.addAll(uvEmptyLineDataSets)

    val lineData = LineData(dataSets)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings(context)

    // Y Axis settings
    with(axisLeft) {
        axisMinimum = 0F
        isGranularityEnabled = true
        granularity = 1F
    }

    with(axisRight) {
        isEnabled = true
        isGranularityEnabled = true
        granularity = 1F
        axisMinimum = 0F
    }

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(uvData.timestamps)
    show()
    notifyDataSetChanged()

    return createDataSetToXPointsMap(
        radiationDataLineDataSetsWithValues.size + radiationDataEmptyLineDataSets.size,
        uvLineDataSetsWithValues
    )
}

@Suppress("MagicNumber")
fun LineChart.initializeNetworkStatsChart(entries: List<Entry>) {
    val dataSet = LineDataSet(entries, "")
    val lineData = LineData(dataSet)
    data = lineData

    // General Chart Settings
    description.isEnabled = false
    legend.isEnabled = false
    setDrawMarkers(false)
    minOffset = 2F

    // Line and highlight Settings
    lineData.setDrawValues(false)
    dataSet.color = context.getColor(R.color.network_stats_chart_primary)
    dataSet.setCircleColor(context.getColor(R.color.network_stats_chart_primary))
    dataSet.setDrawCircleHole(false)
    dataSet.circleRadius = 1F
    dataSet.lineWidth = LINE_WIDTH
    dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
    isHighlightPerDragEnabled = false
    isHighlightPerTapEnabled = false

    // Y Axis settings
    axisLeft.isEnabled = false
    axisRight.isEnabled = false
    xAxis.isEnabled = false
    isScaleYEnabled = false
    isScaleXEnabled = false

    show()
    notifyDataSetChanged()
}

class CustomXAxisFormatter(private val times: MutableList<String>?) : ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return times?.getOrNull(value.toInt()) ?: value.toString()
    }
}

class CustomYAxisFormatter(
    private val decimals: Int = 0,
    private val isAxisLeft: Boolean = true
) : ValueFormatter() {
    @Suppress("MagicNumber")
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        val label = if (decimals > 0 && value < 10000) {
            "${roundToDecimals(value, decimals)}"
        } else if (value < 10000) {
            "${roundToInt(value)}"
        } else {
            // 10000-99999
            "${roundToInt(value / 1000)}K"
        }

        return if (isAxisLeft) {
            label.padStart(Y_AXIS_LABEL_LENGTH, ' ')
        } else {
            label.padEnd(Y_AXIS_LABEL_LENGTH, ' ')
        }
    }
}
