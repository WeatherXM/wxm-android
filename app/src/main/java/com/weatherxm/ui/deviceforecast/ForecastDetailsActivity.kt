package com.weatherxm.ui.deviceforecast

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.Status
import com.weatherxm.data.services.CacheService.Companion.KEY_PRESSURE
import com.weatherxm.data.services.CacheService.Companion.KEY_WIND
import com.weatherxm.databinding.ActivityForecastDetailsBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_FORECAST_SELECTED_DAY
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.HourlyForecastAdapter
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIForecastDay
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.boldText
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.moveItemToCenter
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.screenLocation
import com.weatherxm.ui.common.setColor
import com.weatherxm.ui.common.setDisplayTimezone
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.setWeatherAnimation
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.components.LineChartView
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
    companion object {
        const val SCROLL_DURATION_MS = 500
    }

    private lateinit var binding: ActivityForecastDetailsBinding

    private val model: ForecastDetailsViewModel by viewModel {
        parametersOf(intent.parcelable<UIDevice>(Contracts.ARG_DEVICE))
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

        binding.displayTimeNotice.setDisplayTimezone(model.device.timezone)
        setupChartsAndListeners()

        model.onForecastLoaded().observe(this) {
            when (it.status) {
                Status.SUCCESS -> {
                    val selectedDayPosition = model.getSelectedDayPosition(
                        intent.getStringExtra(ARG_FORECAST_SELECTED_DAY)
                    )
                    val forecastDay = model.forecast().forecastDays[selectedDayPosition]
                    setupDailyAdapter(forecastDay, selectedDayPosition)
                    updateUI(forecastDay)
                    binding.statusView.setVisible(false)
                    binding.mainContainer.setVisible(true)
                }
                Status.ERROR -> {
                    binding.statusView.clear()
                        .animation(R.raw.anim_error)
                        .title(getString(R.string.error_generic_message))
                        .subtitle(it.message)
                    binding.mainContainer.setVisible(false)
                }
                Status.LOADING -> {
                    binding.statusView.clear().animation(R.raw.anim_loading)
                    binding.mainContainer.setVisible(false)
                    binding.statusView.setVisible(true)
                }
            }
        }

        model.fetchForecast()
    }

    private fun updateUI(forecast: UIForecastDay) {
        // Update Daily Weather
        binding.dailyDate.text = forecast.date.getRelativeDayAndShort(this)
        binding.dailyIcon.setWeatherAnimation(forecast.icon)
        binding.dailyMaxTemp.text = getFormattedTemperature(forecast.maxTemp)
        binding.dailyMinTemp.text = getFormattedTemperature(forecast.minTemp)
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

        // Update Hourly Tiles
        hourlyAdapter = HourlyForecastAdapter(null)
        binding.hourlyForecastRecycler.adapter = hourlyAdapter
        hourlyAdapter.submitList(forecast.hourlyWeather)
        if (!forecast.hourlyWeather.isNullOrEmpty()) {
            binding.hourlyForecastRecycler.scrollToPosition(
                model.getDefaultHourPosition(forecast.hourlyWeather)
            )
        }

        // Update Charts
        with(binding.charts) {
            val charts = model.getCharts(forecast)
            clearCharts()
            initTemperatureChart(charts.temperature, charts.feelsLike)
            initWindChart(charts.windSpeed, charts.windGust, charts.windDirection)
            initPrecipitationChart(charts.precipitation, charts.precipProbability, false)
            initHumidityChart(charts.humidity)
            initPressureChart(charts.pressure)
            initSolarChart(charts.uv, charts.solarRadiation)
            autoHighlightCharts(0F)
            setVisible(!charts.isEmpty())
        }
    }

    private fun setupChartsAndListeners() {
        with(binding.charts) {
            chartPrecipitation().primaryLine(
                getString(R.string.precipitation), getString(R.string.precipitation)
            )
            chartPrecipitation().secondaryLine(
                getString(R.string.probability),
                getString(R.string.precipitation_probability)
            )
            chartWind().primaryLine(null, getString(R.string.speed))
            chartWind().secondaryLine(null, null)
            chartSolar().updateTitle(getString(R.string.uv_index))
            chartSolar().primaryLine(null, getString(R.string.uv_index))
            chartSolar().secondaryLine(null, null)
            binding.dailyMainCard.setOnClickListener { scrollToChart(chartTemperature()) }
            binding.precipProbabilityCard.setOnClickListener { scrollToChart(chartPrecipitation()) }
            binding.dailyPrecipCard.setOnClickListener { scrollToChart(chartPrecipitation()) }
            binding.windCard.setOnClickListener { scrollToChart(chartWind()) }
            binding.humidityCard.setOnClickListener { scrollToChart(chartHumidity()) }
            binding.uvCard.setOnClickListener { scrollToChart(chartSolar()) }
            binding.pressureCard.setOnClickListener { scrollToChart(chartPressure()) }
        }
    }

    private fun scrollToChart(chart: LineChartView) {
        val (chartX, chartY) = chart.screenLocation()
        val currentY = binding.mainContainer.scrollY
        val finalY = chartY - binding.appBar.height - binding.root.paddingTop + currentY
        binding.mainContainer.smoothScrollTo(chartX, finalY, SCROLL_DURATION_MS)
    }

    private fun setupDailyAdapter(forecastDay: UIForecastDay, selectedDayPosition: Int) {
        dailyAdapter = DailyTileForecastAdapter(
            forecastDay.date,
            onNewSelectedPosition = { position, width ->
                binding.dailyTilesRecycler.moveItemToCenter(position, binding.root.width, width)
            },
            onClickListener = {
                analytics.trackEventSelectContent(
                    AnalyticsService.ParamValue.DAILY_CARD.paramValue,
                    Pair(
                        FirebaseAnalytics.Param.ITEM_ID,
                        AnalyticsService.ParamValue.DAILY_DETAILS.paramValue
                    )
                )
                // Get selected position before we update it in order to reset the stroke
                dailyAdapter.notifyItemChanged(dailyAdapter.getSelectedPosition())
                updateUI(it)
            }
        )
        binding.dailyTilesRecycler.adapter = dailyAdapter
        dailyAdapter.submitList(model.forecast().forecastDays)
        binding.dailyTilesRecycler.scrollToPosition(selectedDayPosition)
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

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.DEVICE_FORECAST_DETAILS, classSimpleName())
    }
}
