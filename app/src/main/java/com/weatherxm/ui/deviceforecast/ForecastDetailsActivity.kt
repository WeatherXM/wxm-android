package com.weatherxm.ui.deviceforecast

import android.os.Bundle
import com.weatherxm.R
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.services.CacheService.Companion.KEY_PRESSURE
import com.weatherxm.data.services.CacheService.Companion.KEY_WIND
import com.weatherxm.databinding.ActivityForecastDetailsBinding
import com.weatherxm.ui.common.Charts
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_FORECAST_SELECTED_DAY
import com.weatherxm.ui.common.Contracts.ARG_FORECAST_SELECTED_HOUR
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.HourlyForecastAdapter
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.ui.common.UIForecastDay
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.boldText
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.setColor
import com.weatherxm.ui.common.setDisplayTimezone
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.setWeatherAnimation
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.components.LineChartView
import com.weatherxm.util.Analytics
import com.weatherxm.util.DateTimeHelper.getRelativeDayAndShort
import com.weatherxm.util.Weather.getFormattedHumidity
import com.weatherxm.util.Weather.getFormattedPrecipitation
import com.weatherxm.util.Weather.getFormattedPrecipitationProbability
import com.weatherxm.util.Weather.getFormattedPressure
import com.weatherxm.util.Weather.getFormattedTemperature
import com.weatherxm.util.Weather.getFormattedUV
import com.weatherxm.util.Weather.getFormattedWind
import com.weatherxm.util.Weather.getFormattedWindDirection
import com.weatherxm.util.Weather.getPrecipitationPreferredUnit
import com.weatherxm.util.Weather.getPreferredUnit
import com.weatherxm.util.Weather.getUVClassification
import com.weatherxm.util.Weather.getWindDirectionDrawable
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class ForecastDetailsActivity : BaseActivity() {
    private lateinit var binding: ActivityForecastDetailsBinding

    private val model: ForecastDetailsViewModel by viewModel {
        parametersOf(
            intent.parcelable<UIDevice>(Contracts.ARG_DEVICE),
            intent.parcelable<UIForecast>(Contracts.ARG_FORECAST)
        )
    }

    private lateinit var dailyAdapter: DailyTileForecastAdapter
    private lateinit var hourlyAdapter: HourlyForecastAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForecastDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        if (model.device.isEmpty()) {
            Timber.d("Could not start ForecastDetailsActivity. Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val defaultOrFriendlyName = model.device.getDefaultOrFriendlyName()
        binding.header.title(defaultOrFriendlyName)
        if (defaultOrFriendlyName == model.device.name) {
            binding.header.hideSubtitle()
        } else {
            binding.header.subtitle(model.device.name)
        }
        handleOwnershipIcon()

        val selectedHour = intent.parcelable<HourlyWeather>(ARG_FORECAST_SELECTED_HOUR)
        val selectedDayPosition = model.getSelectedDayPosition(
            intent.parcelable<UIForecastDay>(ARG_FORECAST_SELECTED_DAY),
            selectedHour
        )
        val forecastDay = model.forecast.forecastDays[selectedDayPosition]
        setupDailyAdapter(forecastDay, selectedDayPosition)
        hourlyAdapter = HourlyForecastAdapter(null)
        binding.hourlyForecastRecycler.adapter = hourlyAdapter
        setupHourlyAdapter(forecastDay, selectedHour)
        updateDailyWeather(forecastDay)
        binding.charts.chartPrecipitation().primaryLine(
            getString(R.string.precipitation),
            getString(R.string.precipitation)
        )
        binding.charts.chartPrecipitation().secondaryLine(
            getString(R.string.precipitation_probability),
            getString(R.string.precipitation_probability)
        )
        binding.charts.chartWind().primaryLine(null, getString(R.string.speed))
        binding.charts.chartWind().secondaryLine(null, null)
        binding.charts.chartSolar().updateTitle(getString(R.string.uv_index))
        binding.charts.chartSolar().primaryLine(null, getString(R.string.uv_index))
        binding.charts.chartSolar().secondaryLine(null, null)

        updateCharts(model.getCharts(forecastDay))

        binding.precipProbabilityCard.setOnClickListener {
            scrollToChart(binding.charts.chartPrecipitation())
        }
        binding.dailyPrecipCard.setOnClickListener {
            scrollToChart(binding.charts.chartPrecipitation())
        }
        binding.windCard.setOnClickListener { scrollToChart(binding.charts.chartWind()) }
        binding.humidityCard.setOnClickListener { scrollToChart(binding.charts.chartHumidity()) }
        binding.uvCard.setOnClickListener { scrollToChart(binding.charts.chartSolar()) }
        binding.pressureCard.setOnClickListener { scrollToChart(binding.charts.chartPressure()) }

        binding.displayTimeNotice.setDisplayTimezone(model.device.timezone)
    }

    private fun scrollToChart(chart: LineChartView) {
        chart.parent.requestChildFocus(chart, chart)
    }

    private fun setupDailyAdapter(forecastDay: UIForecastDay, selectedDayPosition: Int) {
        dailyAdapter = DailyTileForecastAdapter(forecastDay.date) {
            // Get selected position before we change it to the new one in order to reset the stroke
            dailyAdapter.notifyItemChanged(dailyAdapter.getSelectedPosition())
            updateDailyWeather(it)
            setupHourlyAdapter(it, null)
            updateCharts(model.getCharts(it))
        }
        binding.dailyTilesRecycler.adapter = dailyAdapter
        dailyAdapter.submitList(model.forecast.forecastDays)
        binding.dailyTilesRecycler.scrollToPosition(selectedDayPosition)
    }

    private fun setupHourlyAdapter(forecastDay: UIForecastDay, selectedHour: HourlyWeather?) {
        hourlyAdapter.submitList(forecastDay.hourlyWeather)
        if (!forecastDay.hourlyWeather.isNullOrEmpty()) {
            binding.hourlyForecastRecycler.scrollToPosition(
                model.getSelectedHourPosition(forecastDay.hourlyWeather, selectedHour)
            )
        }
    }

    private fun updateDailyWeather(forecast: UIForecastDay) {
        binding.dailyDate.text = forecast.date.getRelativeDayAndShort(this)
        binding.dailyIcon.setWeatherAnimation(forecast.icon)
        binding.dailyMaxTemp.text = getFormattedTemperature(forecast.maxTemp, 1)
        binding.dailyMinTemp.text = getFormattedTemperature(forecast.minTemp, 1)

        binding.precipProbabilityCard.setData(
            getFormattedPrecipitationProbability(forecast.precipProbability, false), "%"
        )
        val windValue =
            getFormattedWind(forecast.windSpeed, forecast.windDirection, includeUnits = false)
        val windUnit = getPreferredUnit(getString(KEY_WIND), getString(R.string.wind_speed_ms))
        val windDirectionUnit = getFormattedWindDirection(forecast.windDirection)
        val formattedWindUnit = "$windUnit $windDirectionUnit".boldText(windDirectionUnit)
        binding.windCard.setIcon(getWindDirectionDrawable(this, forecast.windDirection))
        binding.windCard.setData(windValue, spannableStringBuilder = formattedWindUnit)

        binding.dailyPrecipCard.setData(
            getFormattedPrecipitation(forecast.precip, isRainRate = false, includeUnit = false),
            getPrecipitationPreferredUnit(false)
        )
        binding.uvCard.setData(getFormattedUV(forecast.uv, false), getUVClassification(forecast.uv))

        binding.humidityCard.setData(
            getFormattedHumidity(forecast.humidity, includeUnit = false), "%"
        )
        val pressureUnit =
            getPreferredUnit(getString(KEY_PRESSURE), getString(R.string.pressure_hpa))
        binding.pressureCard.setData(
            getFormattedPressure(forecast.pressure, includeUnit = false), pressureUnit
        )
    }

    private fun handleOwnershipIcon() {
        with(binding.ownershipIcon) {
            when (model.device.relation) {
                DeviceRelation.OWNED -> {
                    setImageResource(R.drawable.ic_home)
                    setColor(R.color.colorOnSurface)
                }
                DeviceRelation.FOLLOWED -> {
                    setImageResource(R.drawable.ic_favorite)
                    setColor(R.color.follow_heart_color)
                }
                else -> setVisible(false)
            }
        }
    }

    private fun updateCharts(charts: Charts) {
        binding.charts.clearCharts()
        binding.charts.initTemperatureChart(charts.temperature, charts.feelsLike)
        binding.charts.initWindChart(charts.windSpeed, charts.windGust, charts.windDirection)
        binding.charts.initPrecipitationChart(charts.precipitation, charts.precipProbability, false)
        binding.charts.initHumidityChart(charts.humidity)
        binding.charts.initPressureChart(charts.pressure)
        binding.charts.initSolarChart(charts.uv, charts.solarRadiation)
        binding.charts.autoHighlightCharts(0F)
        binding.charts.setVisible(!charts.isEmpty())
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(Analytics.Screen.DEVICE_FORECAST_DETAILS, this::class.simpleName)
    }
}
