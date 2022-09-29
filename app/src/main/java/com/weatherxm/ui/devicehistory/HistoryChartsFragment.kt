package com.weatherxm.ui.devicehistory

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.weatherxm.R
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentHistoryChartsBinding
import com.weatherxm.ui.BarChartData
import com.weatherxm.ui.HistoryCharts
import com.weatherxm.ui.LineChartData
import com.weatherxm.util.initializeHumidity24hChart
import com.weatherxm.util.initializePrecipitation24hChart
import com.weatherxm.util.initializePressure24hChart
import com.weatherxm.util.initializeTemperature24hChart
import com.weatherxm.util.initializeUV24hChart
import com.weatherxm.util.initializeWind24hChart
import timber.log.Timber
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class HistoryChartsFragment : Fragment() {

    fun interface SwipeRefreshCallback {
        fun onSwipeRefresh()
    }

    private val model: HistoryChartsViewModel by activityViewModels()
    private var callback: SwipeRefreshCallback? = null
    private lateinit var binding: FragmentHistoryChartsBinding

    @Suppress("SwallowedException")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            callback = activity as SwipeRefreshCallback
        } catch (e: ClassCastException) {
            Timber.w("${activity?.localClassName} does not implement SwipeRefreshCallback")
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
            visibility = model.device.timezone?.let {
                text = getString(R.string.displayed_times, it)
                View.VISIBLE
            } ?: View.GONE
        }

        binding.swiperefresh.setOnRefreshListener {
            refresh()
        }

        model.charts().observe(viewLifecycleOwner) { resource ->
            Timber.d("Charts updated: ${resource.status}")
            when (resource.status) {
                Status.SUCCESS -> {
                    binding.swiperefresh.isRefreshing = false
                    val isEmpty = resource.data == null || resource.data.isEmpty()
                    if (isEmpty) {
                        binding.chartsView.visibility = View.GONE
                        binding.empty.clear()
                        binding.empty.title(getString(R.string.empty_history_day_title))
                        binding.empty.subtitle(
                            resource.data?.date?.let {
                                getString(
                                    R.string.empty_history_day_subtitle_with_day,
                                    it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                                )
                            } ?: getString(R.string.empty_history_day_subtitle)
                        )
                        binding.empty.animation(R.raw.anim_empty_generic)
                        binding.empty.visibility = View.VISIBLE
                    } else {
                        resource.data?.let { updateUI(it) }
                        binding.empty.visibility = View.GONE
                        binding.chartsView.visibility = View.VISIBLE
                    }
                }
                Status.ERROR -> {
                    Timber.d("Got error: $resource.message")
                    binding.swiperefresh.isRefreshing = false
                    binding.chartsView.visibility = View.GONE
                    binding.empty.clear()
                    binding.empty.animation(R.raw.anim_error)
                    binding.empty.title(getString(R.string.error_history_no_data_on_day))
                    binding.empty.subtitle(resource.message)
                    binding.empty.action(getString(R.string.action_retry))
                    binding.empty.listener { refresh() }
                    binding.empty.visibility = View.VISIBLE
                }
                Status.LOADING -> {
                    if (binding.swiperefresh.isRefreshing) {
                        binding.empty.clear()
                        binding.empty.visibility = View.GONE
                    } else {
                        binding.chartsView.visibility = View.GONE
                        binding.empty.clear()
                        binding.empty.animation(R.raw.anim_loading)
                        binding.empty.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun refresh() {
        callback?.onSwipeRefresh()
    }

    private fun clearCharts() {
        binding.chartTemperature.getChart().clear()
        binding.chartPrecipitation.getChart().clear()
        binding.chartWind.getChart().clear()
        binding.chartHumidity.getChart().clear()
        binding.chartPressure.getChart().clear()
        binding.chartUvIndex.getChart().clear()
    }

    private fun updateUI(historyCharts: HistoryCharts) {
        Timber.d("Updating charts for ${historyCharts.date}")

        clearCharts()

        // Init Temperature Chart
        initTemperatureChart(binding.chartTemperature.getChart(), historyCharts.temperature)

        // Init Wind Chart
        initWindChart(historyCharts.windSpeed, historyCharts.windGust, historyCharts.windDirection)

        // Init Precipitation Chart
        initPrecipitationChart(historyCharts.precipitation)

        // Init Humidity Chart
        initHumidityChart(binding.chartHumidity.getChart(), historyCharts.humidity)

        // Init Pressure Char
        initPressureChart(binding.chartPressure.getChart(), historyCharts.pressure)

        // Init Uv Index Char
        initUvChart(binding.chartUvIndex.getChart(), historyCharts.uvIndex)
    }

    private fun initTemperatureChart(lineChart: LineChart, data: LineChartData) {
        if (data.isDataValid()) {
            lineChart.initializeTemperature24hChart(data)
        } else {
            showNoDataText(lineChart)
        }
    }

    private fun initHumidityChart(lineChart: LineChart, data: LineChartData) {
        if (data.isDataValid()) {
            lineChart.initializeHumidity24hChart(data)
        } else {
            showNoDataText(lineChart)
        }
    }

    private fun initPressureChart(lineChart: LineChart, data: LineChartData) {
        if (data.isDataValid()) {
            lineChart.initializePressure24hChart(data)
        } else {
            showNoDataText(lineChart)
        }
    }

    private fun initUvChart(barChart: BarChart, data: BarChartData) {
        if (data.isDataValid()) {
            barChart.initializeUV24hChart(data)
        } else {
            barChart.setNoDataText(getString(R.string.error_history_no_data_chart_found))
            context?.getColor(R.color.colorOnSurface)?.let {
                barChart.setNoDataTextColor(it)
            }
            barChart.setNoDataTextTypeface(Typeface.DEFAULT_BOLD)
        }
    }

    private fun initPrecipitationChart(data: LineChartData) {
        if (data.isDataValid()) {
            binding.chartPrecipitation.getChart()
                .initializePrecipitation24hChart(data)
        } else {
            showNoDataText(binding.chartPrecipitation.getChart())
        }
    }

    private fun initWindChart(
        windSpeedData: LineChartData, windGustData: LineChartData, windDirectionData: LineChartData
    ) {
        if (windSpeedData.isDataValid()
            && windGustData.isDataValid()
            && windDirectionData.isDataValid()
        ) {
            binding.chartWind
                .getChart()
                .initializeWind24hChart(windSpeedData, windGustData, windDirectionData)
        } else {
            showNoDataText(binding.chartWind.getChart())
        }
    }

    private fun showNoDataText(lineChart: LineChart) {
        lineChart.setNoDataText(getString(R.string.error_history_no_data_chart_found))
        context?.getColor(R.color.colorOnSurface)?.let {
            lineChart.setNoDataTextColor(it)
        }
        lineChart.setNoDataTextTypeface(Typeface.DEFAULT_BOLD)
    }
}
