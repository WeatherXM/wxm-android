package com.weatherxm.ui.devicehistory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.weatherxm.R
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentHistoryChartsBinding
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.util.Weather
import com.weatherxm.util.initHumidity24hChart
import com.weatherxm.util.initPrecipitation24hChart
import com.weatherxm.util.initPressure24hChart
import com.weatherxm.util.initSolarChart
import com.weatherxm.util.initTemperature24hChart
import com.weatherxm.util.initWind24hChart
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber
import java.time.format.DateTimeFormatter.ofLocalizedDate
import java.time.format.FormatStyle

class HistoryChartsFragment : BaseFragment() {

    fun interface SwipeRefreshCallback {
        fun onSwipeRefresh()
    }

    private val model: HistoryChartsViewModel by activityViewModel()
    private var callback: SwipeRefreshCallback? = null
    private lateinit var binding: FragmentHistoryChartsBinding

    private var onAutoHighlighting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            callback = activity as SwipeRefreshCallback
        } catch (e: ClassCastException) {
            Timber.w(e, "${activity?.localClassName} does not implement SwipeRefreshCallback")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentHistoryChartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding.displayTimeNotice) {
            model.device.timezone?.let {
                text = getString(R.string.displayed_times, it)
                setVisible(true)
            } ?: setVisible(false)
        }

        binding.swiperefresh.setOnRefreshListener {
            callback?.onSwipeRefresh()
        }

        model.onNewDate().observe(viewLifecycleOwner) {
            binding.chartsView.scrollTo(0, 0)
        }

        model.charts().observe(viewLifecycleOwner) { resource ->
            Timber.d("Charts updated: ${resource.status}")
            when (resource.status) {
                Status.SUCCESS -> {
                    binding.swiperefresh.isRefreshing = false
                    val data = resource.data
                    if (data == null || data.isEmpty()) {
                        binding.chartsView.setVisible(false)
                        binding.empty.clear()
                            .title(getString(R.string.empty_history_day_title))
                            .subtitle(
                                resource.data?.date?.let {
                                    getString(
                                        R.string.empty_history_day_subtitle_with_day,
                                        it.format(ofLocalizedDate(FormatStyle.MEDIUM))
                                    )
                                } ?: getString(R.string.empty_history_day_subtitle)
                            )
                            .animation(R.raw.anim_empty_generic)
                            .setVisible(true)
                    } else {
                        Timber.d("Updating charts for ${data.date}")
                        clearCharts()
                        initTemperatureChart(data.temperature, data.feelsLike)
                        initWindChart(data.windSpeed, data.windGust, data.windDirection)
                        initPrecipitationChart(data.precipitation, data.precipitationAccumulated)
                        initHumidityChart(data.humidity)
                        initPressureChart(data.pressure)
                        initSolarChart(data.uv, data.solarRadiation)
                        if (model.isTodayShown()) {
                            // Auto highlight latest entry
                            autoHighlightCharts(model.getLatestChartEntry(data.temperature))
                        } else {
                            // Auto highlight past dates on 00:00
                            autoHighlightCharts(0F)
                        }
                        binding.empty.setVisible(false)
                        binding.chartsView.setVisible(true)
                    }
                }
                Status.ERROR -> {
                    Timber.d("Got error: $resource.message")
                    binding.swiperefresh.isRefreshing = false
                    binding.chartsView.setVisible(false)
                    binding.empty.clear()
                        .animation(R.raw.anim_error)
                        .title(getString(R.string.error_history_no_data_on_day))
                        .subtitle(resource.message)
                        .action(getString(R.string.action_retry))
                        .listener { callback?.onSwipeRefresh() }
                        .setVisible(true)
                }
                Status.LOADING -> {
                    if (binding.swiperefresh.isRefreshing) {
                        binding.empty.clear().setVisible(false)
                    } else {
                        binding.chartsView.visibility = View.GONE
                        binding.empty.clear().animation(R.raw.anim_loading).setVisible(true)
                    }
                }
            }
        }
    }

    private fun clearCharts() {
        binding.chartTemperature.clearChart()
        binding.chartPressure.clearChart()
        binding.chartHumidity.clearChart()
        binding.chartPrecipitation.clearChart()
        binding.chartWind.clearChart()
        binding.chartSolar.clearChart()
    }

    private fun initTemperatureChart(temperatureData: LineChartData, feelsLikeData: LineChartData) {
        if (temperatureData.isDataValid() && feelsLikeData.isDataValid()) {
            model.temperatureDataSets = binding.chartTemperature
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
                                e.y, decimals = 1, ignoreConversion = true
                            )
                            val feelsLike = Weather.getFormattedTemperature(
                                feelsLikeData.entries[e.x.toInt()].y,
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

    private fun initHumidityChart(data: LineChartData) {
        if (data.isDataValid()) {
            model.humidityDataSets = binding.chartHumidity.getChart().initHumidity24hChart(data)
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

    private fun initPressureChart(data: LineChartData) {
        if (data.isDataValid()) {
            model.pressureDataSets = binding.chartPressure.getChart().initPressure24hChart(data)
            binding.chartPressure.getChart().setOnChartValueSelectedListener(
                object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        if (e != null && !e.y.isNaN()) {
                            val time = data.timestamps[e.x.toInt()]
                            val pressure =
                                Weather.getFormattedPressure(e.y, ignoreConversion = true)
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

    private fun initSolarChart(uvData: LineChartData, radiationData: LineChartData) {
        if (uvData.isDataValid() && radiationData.isDataValid()) {
            model.solarDataSets =
                binding.chartSolar.getChart().initSolarChart(uvData, radiationData)
            binding.chartSolar.getChart().setOnChartValueSelectedListener(
                object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        if (e != null && !e.y.isNaN()) {
                            val time = uvData.timestamps[e.x.toInt()]
                            val uv = Weather.getFormattedUV(e.y.toInt())
                            val radiation = Weather.getFormattedSolarRadiation(
                                radiationData.entries[e.x.toInt()].y
                            )
                            binding.chartSolar.onHighlightedData(time, uv, radiation)

                            autoHighlightCharts(e.x)
                        } else {
                            binding.chartSolar.onClearHighlight()
                        }
                    }

                    override fun onNothingSelected() {
                        // Do Nothing
                    }
                })
        } else {
            binding.chartSolar.showNoDataText()
        }
    }

    private fun initPrecipitationChart(rateData: LineChartData, accumulatedData: LineChartData) {
        if (rateData.isDataValid() && accumulatedData.isDataValid()) {
            model.precipDataSets = binding.chartPrecipitation
                .getChart()
                .initPrecipitation24hChart(rateData, accumulatedData)
            binding.chartPrecipitation.getChart().setOnChartValueSelectedListener(
                object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        if (e != null && !e.y.isNaN()) {
                            val time = accumulatedData.timestamps[e.x.toInt()]
                            val accumulated = Weather.getFormattedPrecipitation(
                                accumulatedData.entries[e.x.toInt()].y,
                                isRainRate = false,
                                ignoreConversion = true
                            )
                            val rate = Weather.getFormattedPrecipitation(
                                rateData.entries[e.x.toInt()].y,
                                isRainRate = true,
                                ignoreConversion = true
                            )
                            binding.chartPrecipitation.onHighlightedData(time, rate, accumulated)

                            autoHighlightCharts(e.x)
                        } else {
                            binding.chartPrecipitation.onClearHighlight()
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

    private fun initWindChart(
        windSpeedData: LineChartData, windGustData: LineChartData, windDirectionData: LineChartData
    ) {
        if (windSpeedData.isDataValid()
            && windGustData.isDataValid()
            && windDirectionData.isDataValid()
        ) {
            model.windDataSets = binding.chartWind
                .getChart()
                .initWind24hChart(windSpeedData, windGustData)
            binding.chartWind.getChart().setOnChartValueSelectedListener(
                object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        if (e != null && !e.y.isNaN()) {
                            val time = windSpeedData.timestamps[e.x.toInt()]
                            val windSpeed = Weather.getFormattedWind(
                                e.y,
                                windDirectionData.entries[e.x.toInt()].y.toInt(),
                                ignoreConversion = true
                            )
                            val windGust = Weather.getFormattedWind(
                                windGustData.entries[e.x.toInt()].y,
                                windDirectionData.entries[e.x.toInt()].y.toInt(),
                                ignoreConversion = true
                            )
                            binding.chartWind.onHighlightedData(time, windSpeed, windGust)

                            autoHighlightCharts(e.x)
                        } else {
                            binding.chartWind.onClearHighlight()
                        }
                    }

                    override fun onNothingSelected() {
                        // Do Nothing
                    }
                })
        } else {
            binding.chartWind.showNoDataText()
        }
    }

    private fun autoHighlightCharts(x: Float) {
        if (onAutoHighlighting) return
        onAutoHighlighting = true
        with(binding) {
            val temperatureDataSetIndex = model.getDataSetIndexForHighlight(
                x, model.temperatureDataSets, chartTemperature.getDatasetsSize() / 2
            )
            val precipDataSetIndex = model.getDataSetIndexForHighlight(
                x, model.precipDataSets, chartPrecipitation.getDatasetsSize() / 2
            )
            val windDataSetIndex = model.getDataSetIndexForHighlight(
                x, model.windDataSets, chartWind.getDatasetsSize() / 2
            )
            val humidityDataSetIndex =
                model.getDataSetIndexForHighlight(x, model.humidityDataSets, 0)
            val pressureDataSetIndex =
                model.getDataSetIndexForHighlight(x, model.pressureDataSets, 0)
            val solarDataSetIndex = model.getDataSetIndexForHighlight(
                x, model.solarDataSets, chartSolar.getDatasetsSize() / 2
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
}
