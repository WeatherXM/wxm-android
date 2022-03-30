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
import com.weatherxm.data.Device
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
import org.koin.core.component.KoinComponent
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class HistoryChartsFragment : Fragment(), KoinComponent {

    private val model: HistoryChartsViewModel by activityViewModels()
    private lateinit var binding: FragmentHistoryChartsBinding
    private lateinit var device: Device

    companion object {
        const val ARG_DEVICE = "device"

        fun newInstance(device: Device) = HistoryChartsFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_DEVICE, device)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        device = requireNotNull(requireArguments().getParcelable(ARG_DEVICE))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentHistoryChartsBinding.inflate(inflater, container, false)

        model.onCharts().observe(viewLifecycleOwner) { resource ->
            Timber.d("Charts updated: ${resource.status}")
            when (resource.status) {
                Status.SUCCESS -> {
                    resource.data?.let { updateUI(it) }
                }
                Status.ERROR -> {
                    Timber.d("Got error: $resource.message")
                    binding.chartsView.visibility = View.GONE
                    binding.empty.clear()
                    binding.empty.animation(R.raw.anim_error)
                    binding.empty.title(getString(R.string.device_history_day_error_title))
                    binding.empty.subtitle(resource.message)
                    binding.empty.action(getString(R.string.action_retry))
                    binding.empty.listener { getWeatherHistory() }
                    binding.empty.visibility = View.VISIBLE
                }
                Status.LOADING -> {
                    binding.chartsView.visibility = View.GONE
                    binding.empty.clear()
                    binding.empty.animation(R.raw.anim_loading)
                    binding.empty.visibility = View.VISIBLE
                }
            }
        }

        return binding.root
    }

    private fun getWeatherHistory() {
        context?.let {
            model.getWeatherHistory(device, it)
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
        binding.chartPressure.getChart().clear()
        binding.chartUvIndex.getChart().clear()
    }

    private fun updateUI(historyCharts: HistoryCharts) {
        clearCharts()

        if (historyCharts.isEmpty()) {
            binding.chartsView.visibility = View.GONE
            binding.empty.clear()
            binding.empty.title(getString(R.string.device_history_day_empty_title))
            binding.empty.subtitle(
                historyCharts.date?.let {
                    getString(
                        R.string.device_history_day_empty_subtitle_with_day,
                        LocalDate.parse(it).format(
                            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                        )
                    )
                } ?: getString(R.string.device_history_day_empty_subtitle)
            )
            binding.empty.animation(R.raw.anim_empty_generic)
            binding.empty.visibility = View.VISIBLE
            return
        } else {
            binding.empty.visibility = View.GONE
            binding.chartsView.visibility = View.VISIBLE
        }

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

        binding.chartsView.visibility = View.VISIBLE
    }

    private fun initTemperatureChart(lineChart: LineChart, data: LineChartData) {
        if (model.isDataValid(data)) {
            lineChart.initializeTemperature24hChart(data)
        } else {
            showNoDataText(lineChart)
        }
    }

    private fun initHumidityChart(lineChart: LineChart, data: LineChartData) {
        if (model.isDataValid(data)) {
            lineChart.initializeHumidity24hChart(data)
        } else {
            showNoDataText(lineChart)
        }
    }

    private fun initPressureChart(lineChart: LineChart, data: LineChartData) {
        if (model.isDataValid(data)) {
            lineChart.initializePressure24hChart(data)
        } else {
            showNoDataText(lineChart)
        }
    }

    private fun initUvChart(barChart: BarChart, data: BarChartData) {
        if (model.isDataValid(data)) {
            barChart.initializeUV24hChart(data)
        } else {
            barChart.setNoDataText(getString(R.string.no_data_chart_found))
            context?.getColor(R.color.black)?.let { barChart.setNoDataTextColor(it) }
            barChart.setNoDataTextTypeface(Typeface.DEFAULT_BOLD)
        }
    }

    private fun initPrecipitationChart(
        precipIntensityData: LineChartData
    ) {
        if (model.isDataValid(precipIntensityData)) {
            binding.chartPrecipitation.getChart()
                .initializePrecipitation24hChart(precipIntensityData)
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
        context?.getColor(R.color.black)?.let { lineChart.setNoDataTextColor(it) }
        lineChart.setNoDataTextTypeface(Typeface.DEFAULT_BOLD)
    }
}
