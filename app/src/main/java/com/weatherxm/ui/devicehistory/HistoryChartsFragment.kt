package com.weatherxm.ui.devicehistory

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentHistoryChartsBinding
import com.weatherxm.ui.BarChartData
import com.weatherxm.ui.HistoryCharts
import com.weatherxm.ui.LineChartData
import com.weatherxm.util.initializeDefault24hChart
import com.weatherxm.util.initializePrecipitation24hChart
import com.weatherxm.util.initializeWind24hChart
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import timber.log.Timber

class HistoryChartsFragment : Fragment(), KoinComponent {

    private val model: HistoryChartsViewModel by activityViewModels()
    private lateinit var binding: FragmentHistoryChartsBinding
    private var device: Device? = null

    companion object {
        const val ARG_DEVICE = "device"

        fun newInstance(device: Device?) = HistoryChartsFragment().apply {
            arguments = Bundle().apply { putParcelable(ARG_DEVICE, device) }
        }
    }

    init {
        lifecycleScope.launch {
            whenCreated {
                device = arguments?.getParcelable(ARG_DEVICE)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentHistoryChartsBinding.inflate(inflater, container, false)

        model.onCharts().observe(viewLifecycleOwner, { resource ->
            Timber.d("Charts updated: ${resource.status}")
            when (resource.status) {
                Status.SUCCESS -> {
                    resource.data?.let { updateUI(it) }
                    binding.progress.visibility = View.GONE
                    binding.chartsView.visibility = View.VISIBLE
                    binding.empty.visibility = View.GONE
                }
                Status.ERROR -> {
                    Timber.d("Got error: $resource.message")
                    binding.progress.visibility = View.GONE
                    binding.chartsView.visibility = View.GONE
                    binding.empty.title(getString(R.string.no_charts_found))
                    binding.empty.subtitle(resource.message)
                    binding.empty.action(getString(R.string.action_retry))
                    binding.empty.listener { getWeatherHistory() }
                    binding.empty.visibility = View.VISIBLE
                }
                Status.LOADING -> {
                    binding.progress.visibility = View.VISIBLE
                    binding.chartsView.visibility = View.GONE
                    binding.empty.visibility = View.GONE
                }
            }
        })

        return binding.root
    }

    private fun getWeatherHistory() {
        device?.let { deviceNonNull ->
            context?.let { contextNonNull ->
                model.getWeatherHistory(deviceNonNull, contextNonNull)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getWeatherHistory()
    }

    private fun clearCharts() {
        binding.chartTemperature.getChart().clear()
        binding.chartPrecipitation.getChart().clear()
        binding.chartWind.getChart().clear()
        binding.chartHumidity.getChart().clear()
        binding.chartCloudCover.getChart().clear()
        binding.chartPressure.getChart().clear()
        binding.chartUvIndex.getChart().clear()
    }

    private fun updateUI(historyCharts: HistoryCharts) {
        clearCharts()

        // Init Temperature Chart
        initDefaultChart(binding.chartTemperature.getChart(), historyCharts.temperature, null)

        // Init Wind Chart
        initWindChart(historyCharts.windSpeed, historyCharts.windGust, historyCharts.windDirection)

        // Init Precipitation Chart
        initPrecipitationChart(historyCharts.precipitation)

        // Init Humidity Chart
        initDefaultChart(binding.chartHumidity.getChart(), historyCharts.humidity, null)

        /*
            Init Cloud Cover Chart, we use yMinValue so the Y Axis starts from 0
            and not going negative when cloud cover is 0%
        */
        initDefaultChart(binding.chartCloudCover.getChart(), historyCharts.cloudCover, 0F)

        // Init Pressure Char
        initDefaultChart(binding.chartPressure.getChart(), historyCharts.pressure, null)

        // Init Uv Index Char
        initUvChart(binding.chartUvIndex.getChart(), historyCharts.uvIndex)
    }

    private fun initDefaultChart(lineChart: LineChart, data: LineChartData, yMinValue: Float?) {
        if (model.isDataValid(data)) {
            lineChart.initializeDefault24hChart(data, yMinValue)
        } else {
            showNoDataText(lineChart)
        }
    }

    private fun initUvChart(barChart: BarChart, data: BarChartData) {
        if (model.isDataValid(data)) {
            barChart.initializeDefault24hChart(data)
        } else {
            barChart.setNoDataText(getString(R.string.no_data_chart_found))
            context?.getColor(R.color.red)?.let { barChart.setNoDataTextColor(it) }
            barChart.setNoDataTextTypeface(Typeface.DEFAULT_BOLD)
        }
    }

    private fun initPrecipitationChart(
        precipIntensityData: LineChartData
    ) {
        if (model.isDataValid(precipIntensityData)) {
            // precipProbabilityData = null because we don't have that data on History charts
            binding.chartPrecipitation.getChart()
                .initializePrecipitation24hChart(precipIntensityData, null)
        } else {
            showNoDataText(binding.chartPrecipitation.getChart())
        }
    }

    private fun initWindChart(
        windSpeedData: LineChartData, windGustData: LineChartData, windDirectionData: LineChartData
    ) {
        if (model.isDataValid(windSpeedData) && model.isDataValid(windGustData)
            && model.isDataValid(windDirectionData)
        ) {
            binding.chartWind
                .getChart()
                .initializeWind24hChart(windSpeedData, windGustData, windDirectionData)
        } else {
            showNoDataText(binding.chartWind.getChart())
        }
    }

    private fun showNoDataText(lineChart: LineChart) {
        lineChart.setNoDataText(getString(R.string.no_data_chart_found))
        context?.getColor(R.color.red)?.let { lineChart.setNoDataTextColor(it) }
        lineChart.setNoDataTextTypeface(Typeface.DEFAULT_BOLD)
    }
}
