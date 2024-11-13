package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.weatherxm.databinding.ViewChartsBinding
import com.weatherxm.ui.common.LineChartData
import com.weatherxm.util.Weather
import com.weatherxm.util.Weather.getFormattedSolarRadiation
import com.weatherxm.util.initHumidity24hChart
import com.weatherxm.util.initPrecipitation24hChart
import com.weatherxm.util.initPressure24hChart
import com.weatherxm.util.initSolarChart
import com.weatherxm.util.initTemperature24hChart
import com.weatherxm.util.initWind24hChart

@Suppress("TooManyFunctions")
class ChartsView : LinearLayout {

    private lateinit var binding: ViewChartsBinding

    private var temperatureDataSets: MutableMap<Int, List<Float>> = mutableMapOf()
    private var precipDataSets: MutableMap<Int, List<Float>> = mutableMapOf()
    private var windDataSets: MutableMap<Int, List<Float>> = mutableMapOf()
    private var humidityDataSets: MutableMap<Int, List<Float>> = mutableMapOf()
    private var pressureDataSets: MutableMap<Int, List<Float>> = mutableMapOf()
    private var solarDataSets: MutableMap<Int, List<Float>> = mutableMapOf()

    private var onAutoHighlighting = false

    constructor(context: Context?) : super(context) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context?) {
        binding = ViewChartsBinding.inflate(LayoutInflater.from(context), this)
    }

    fun chartTemperature() = binding.chartTemperature
    fun chartPrecipitation() = binding.chartPrecipitation
    fun chartWind() = binding.chartWind
    fun chartSolar() = binding.chartSolar
    fun chartHumidity() = binding.chartHumidity
    fun chartPressure() = binding.chartPressure

    fun clearCharts() {
        binding.chartTemperature.clearChart()
        binding.chartPressure.clearChart()
        binding.chartHumidity.clearChart()
        binding.chartPrecipitation.clearChart()
        binding.chartWind.clearChart()
        binding.chartSolar.clearChart()
    }

    fun initTemperatureChart(temperatureData: LineChartData, feelsLikeData: LineChartData) {
        if (temperatureData.isDataValid() && feelsLikeData.isDataValid()) {
            temperatureDataSets = binding.chartTemperature
                .getChart()
                .initTemperature24hChart(temperatureData, feelsLikeData)
            binding.chartTemperature.getChart().setOnChartValueSelectedListener(
                object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        if (e != null && !e.y.isNaN()) {
                            val time = temperatureData.timestamps[e.x.toInt()]

                            /**
                             * Ignore conversion of the temperature because the data on the Y axis
                             * is already converted to the user's preference so we need to handle
                             * only the decimals
                             */
                            val temperature = Weather.getFormattedTemperature(
                                context = context,
                                value = e.y,
                                decimals = 1,
                                ignoreConversion = true
                            )
                            val feelsLike = Weather.getFormattedTemperature(
                                context = context,
                                value = feelsLikeData.entries[e.x.toInt()].y,
                                decimals = 1,
                                ignoreConversion = true
                            )
                            binding.chartTemperature.onHighlightedData(time, temperature, feelsLike)

                            autoHighlightCharts(e.x)
                        } else {
                            binding.chartTemperature.onClearHighlight()
                        }
                    }

                    override fun onNothingSelected() {
                        // Do Nothing
                    }
                })
        } else {
            binding.chartTemperature.showNoDataText()
        }
    }

    fun initHumidityChart(data: LineChartData) {
        if (data.isDataValid()) {
            humidityDataSets = binding.chartHumidity.getChart().initHumidity24hChart(data)
            binding.chartHumidity.getChart().setOnChartValueSelectedListener(
                object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        if (e != null && !e.y.isNaN()) {
                            val time = data.timestamps[e.x.toInt()]
                            val humidity = Weather.getFormattedHumidity(e.y.toInt())
                            binding.chartHumidity.onHighlightedData(time, humidity)

                            autoHighlightCharts(e.x)
                        } else {
                            binding.chartHumidity.onClearHighlight()
                        }
                    }

                    override fun onNothingSelected() {
                        // Do Nothing
                    }
                })
        } else {
            binding.chartHumidity.showNoDataText()
        }
    }

    fun initPressureChart(data: LineChartData) {
        if (data.isDataValid()) {
            pressureDataSets = binding.chartPressure.getChart().initPressure24hChart(data)
            binding.chartPressure.getChart().setOnChartValueSelectedListener(
                object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        if (e != null && !e.y.isNaN()) {
                            val time = data.timestamps[e.x.toInt()]
                            val pressure = Weather.getFormattedPressure(
                                context = context,
                                value = e.y,
                                ignoreConversion = true
                            )
                            binding.chartPressure.onHighlightedData(time, pressure)

                            autoHighlightCharts(e.x)
                        } else {
                            binding.chartPressure.onClearHighlight()
                        }
                    }

                    override fun onNothingSelected() {
                        // Do Nothing
                    }
                })
        } else {
            binding.chartPressure.showNoDataText()
        }
    }

    fun initSolarChart(uvData: LineChartData, radiationData: LineChartData) {
        if (uvData.isDataValid() || radiationData.isDataValid()) {
            solarDataSets = binding.chartSolar.getChart().initSolarChart(uvData, radiationData)
            binding.chartSolar.getChart().setOnChartValueSelectedListener(
                object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        onSolarValueSelected(uvData, radiationData, e)
                    }

                    override fun onNothingSelected() {
                        // Do Nothing
                    }
                })
        } else {
            binding.chartSolar.showNoDataText()
        }
    }

    fun initPrecipitationChart(
        primaryData: LineChartData,
        secondaryData: LineChartData,
        isHistoricalData: Boolean
    ) {
        if (primaryData.isDataValid() && secondaryData.isDataValid()) {
            precipDataSets = binding.chartPrecipitation
                .getChart()
                .initPrecipitation24hChart(primaryData, secondaryData, isHistoricalData)
            binding.chartPrecipitation.getChart().setOnChartValueSelectedListener(
                object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        if (isHistoricalData) {
                            onHistoricalPrecipitationValueSelected(primaryData, secondaryData, e)
                        } else {
                            onForecastPrecipitationValueSelected(primaryData, secondaryData, e)
                        }
                    }

                    override fun onNothingSelected() {
                        // Do Nothing
                    }
                })
        } else {
            binding.chartPrecipitation.showNoDataText()
        }
    }

    fun initWindChart(
        windSpeedData: LineChartData, windGustData: LineChartData, windDirectionData: LineChartData
    ) {
        if (windSpeedData.isDataValid() && windDirectionData.isDataValid()) {
            windDataSets = binding.chartWind
                .getChart()
                .initWind24hChart(windSpeedData, windGustData)
            binding.chartWind.getChart().setOnChartValueSelectedListener(
                object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        onWindValueSelected(windSpeedData, windGustData, windDirectionData, e)
                    }

                    override fun onNothingSelected() {
                        // Do Nothing
                    }
                })
        } else {
            binding.chartWind.showNoDataText()
        }
    }

    private fun onSolarValueSelected(
        uvData: LineChartData,
        radiationData: LineChartData,
        e: Entry?
    ) {
        if (e != null && !e.y.isNaN()) {
            val time = uvData.timestamps[e.x.toInt()]
            val uv = Weather.getFormattedUV(context, e.y.toInt())

            if (radiationData.isDataValid()) {
                val radiation =
                    getFormattedSolarRadiation(context, radiationData.entries[e.x.toInt()].y)
                binding.chartSolar.onHighlightedData(time, uv, radiation)
            } else {
                binding.chartSolar.onHighlightedData(time, uv)
            }

            autoHighlightCharts(e.x)
        } else {
            binding.chartSolar.onClearHighlight()
        }
    }

    private fun onHistoricalPrecipitationValueSelected(
        primaryData: LineChartData,
        secondaryData: LineChartData,
        e: Entry?
    ) {
        if (e != null && !e.y.isNaN()) {
            val time = secondaryData.timestamps[e.x.toInt()]
            val accumulated = Weather.getFormattedPrecipitation(
                context = context,
                value = secondaryData.entries[e.x.toInt()].y,
                isRainRate = false,
                ignoreConversion = true
            )
            val rate = Weather.getFormattedPrecipitation(
                context = context,
                value = primaryData.entries[e.x.toInt()].y,
                isRainRate = true,
                ignoreConversion = true
            )
            binding.chartPrecipitation.onHighlightedData(time, rate, accumulated)

            autoHighlightCharts(e.x)
        } else {
            binding.chartPrecipitation.onClearHighlight()
        }
    }

    private fun onForecastPrecipitationValueSelected(
        primaryData: LineChartData,
        secondaryData: LineChartData,
        e: Entry?
    ) {
        if (e != null && !e.y.isNaN()) {
            val time = secondaryData.timestamps[e.x.toInt()]
            val precipitation = Weather.getFormattedPrecipitation(
                context = context,
                value = primaryData.entries[e.x.toInt()].y,
                isRainRate = false,
                ignoreConversion = true
            )
            val percentage = Weather.getFormattedPrecipitationProbability(
                secondaryData.entries[e.x.toInt()].y.toInt()
            )
            binding.chartPrecipitation.onHighlightedData(time, precipitation, percentage)

            autoHighlightCharts(e.x)
        } else {
            binding.chartPrecipitation.onClearHighlight()
        }
    }

    private fun onWindValueSelected(
        windSpeedData: LineChartData,
        windGustData: LineChartData,
        windDirectionData: LineChartData,
        e: Entry?
    ) {
        if (e != null && !e.y.isNaN()) {
            val time = windSpeedData.timestamps[e.x.toInt()]
            val windSpeed = Weather.getFormattedWind(
                context = context,
                windSpeed = e.y,
                windDirection = windDirectionData.entries[e.x.toInt()].y.toInt(),
                ignoreConversion = true
            )

            if (windGustData.isDataValid()) {
                val windGust = Weather.getFormattedWind(
                    context = context,
                    windSpeed = windGustData.entries[e.x.toInt()].y,
                    windDirection = windDirectionData.entries[e.x.toInt()].y.toInt(),
                    ignoreConversion = true
                )
                binding.chartWind.onHighlightedData(time, windSpeed, windGust)
            } else {
                binding.chartWind.onHighlightedData(time, windSpeed)
            }

            autoHighlightCharts(e.x)
        } else {
            binding.chartWind.onClearHighlight()
        }
    }

    fun autoHighlightCharts(x: Float) {
        if (onAutoHighlighting) return
        onAutoHighlighting = true
        with(binding) {
            val temperatureDataSetIndex = getDataSetIndexForHighlight(
                x, temperatureDataSets, chartTemperature.getDatasetsSize() / 2
            )
            val precipDataSetIndex = getDataSetIndexForHighlight(
                x, precipDataSets, chartPrecipitation.getDatasetsSize() / 2
            )
            val windDataSetIndex = getDataSetIndexForHighlight(
                x, windDataSets, chartWind.getDatasetsSize() / 2
            )
            val humidityDataSetIndex =
                getDataSetIndexForHighlight(x, humidityDataSets, 0)
            val pressureDataSetIndex =
                getDataSetIndexForHighlight(x, pressureDataSets, 0)
            val solarDataSetIndex = getDataSetIndexForHighlight(
                x, solarDataSets, chartSolar.getDatasetsSize() / 2
            )

            chartTemperature.onHighlightValue(x, temperatureDataSetIndex)
            chartPrecipitation.onHighlightValue(x, precipDataSetIndex)
            chartWind.onHighlightValue(x, windDataSetIndex)
            chartHumidity.onHighlightValue(x, humidityDataSetIndex)
            chartPressure.onHighlightValue(x, pressureDataSetIndex)
            chartSolar.onHighlightValue(x, solarDataSetIndex)
        }
        onAutoHighlighting = false
    }

    fun getLatestChartEntry(lineChartData: LineChartData): Float {
        val firstNaN = lineChartData.entries.firstOrNull { it.y.isNaN() }?.x
        return if (firstNaN != null && firstNaN > 0F) {
            firstNaN - 1
        } else {
            0F
        }
    }

    private fun getDataSetIndexForHighlight(
        x: Float,
        dataSet: MutableMap<Int, List<Float>>,
        fallback: Int
    ): Int {
        return dataSet.filterValues {
            it.contains(x)
        }.keys.let {
            if (it.isNotEmpty()) {
                it.first()
            } else {
                fallback
            }
        }
    }
}
