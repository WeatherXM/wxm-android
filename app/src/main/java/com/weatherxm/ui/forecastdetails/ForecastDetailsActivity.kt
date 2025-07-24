package com.weatherxm.ui.forecastdetails

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityForecastDetailsBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_FORECAST_SELECTED_DAY
import com.weatherxm.ui.common.Contracts.EMPTY_VALUE
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.HourlyForecastAdapter
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIForecastDay
import com.weatherxm.ui.common.UILocation
import com.weatherxm.ui.common.capitalizeWords
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.moveItemToCenter
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.screenLocation
import com.weatherxm.ui.common.setColor
import com.weatherxm.ui.common.setDisplayTimezone
import com.weatherxm.ui.common.setWeatherAnimation
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.components.LineChartView
import com.weatherxm.ui.components.ProPromotionDialogFragment
import com.weatherxm.ui.components.compose.HeaderView
import com.weatherxm.ui.components.compose.ProPromotionCard
import com.weatherxm.util.DateTimeHelper.getRelativeDayAndShort
import com.weatherxm.util.Weather.getFormattedHumidity
import com.weatherxm.util.Weather.getFormattedPrecipitation
import com.weatherxm.util.Weather.getFormattedPrecipitationProbability
import com.weatherxm.util.Weather.getFormattedPressure
import com.weatherxm.util.Weather.getFormattedTemperature
import com.weatherxm.util.Weather.getFormattedUV
import com.weatherxm.util.Weather.getFormattedWind
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
        parametersOf(
            intent.parcelable<UIDevice>(Contracts.ARG_DEVICE),
            intent.parcelable<UILocation>(Contracts.ARG_LOCATION)
        )
    }

    private lateinit var dailyAdapter: DailyTileForecastAdapter
    private lateinit var hourlyAdapter: HourlyForecastAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForecastDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (model.device.isEmpty() && model.location.isEmpty()) {
            Timber.d("Could not start ForecastDetailsActivity. Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        if (!model.device.isEmpty()) {
            binding.header.setContent {
                val defaultOrFriendlyName = model.device.getDefaultOrFriendlyName()
                val subtitle = if (defaultOrFriendlyName == model.device.name) {
                    null
                } else {
                    model.device.name
                }
                HeaderView(defaultOrFriendlyName, subtitle, null)
            }

            handleOwnershipIcon()

            binding.displayTimeNotice.setDisplayTimezone(model.device.timezone)
        } else {
            handleSavedLocationIcon()
            binding.displayTimeNotice.visible(false)
        }
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
                    binding.statusView.visible(false)
                    binding.mainContainer.visible(true)
                }
                Status.ERROR -> {
                    binding.statusView.clear()
                        .animation(R.raw.anim_error)
                        .title(getString(R.string.error_generic_message))
                        .subtitle(it.message)
                    binding.mainContainer.visible(false)
                }
                Status.LOADING -> {
                    binding.statusView.clear().animation(R.raw.anim_loading)
                    binding.mainContainer.visible(false)
                    binding.statusView.visible(true)
                }
            }
        }

        binding.proPromotionCard.setContent {
            ProPromotionCard(R.string.want_more_accurate_forecasts) {
                analytics.trackEventSelectContent(
                    AnalyticsService.ParamValue.PRO_PROMOTION_CTA.paramValue,
                    Pair(
                        FirebaseAnalytics.Param.SOURCE,
                        AnalyticsService.ParamValue.LOCAL_FORECAST_DETAILS.paramValue
                    )
                )
                ProPromotionDialogFragment().show(this)
            }
        }

        if (!model.device.isEmpty()) {
            model.fetchDeviceForecast()
        } else if (!model.location.isEmpty()) {
            model.fetchLocationForecast()
        }
    }

    private fun updateUI(forecast: UIForecastDay) {
        // Update the header now that model.address has valid data
        binding.header.setContent {
            if (model.location.isCurrentLocation) {
                HeaderView(
                    title = getString(R.string.current_location).capitalizeWords(),
                    subtitle = model.address(),
                    onInfoButton = null
                )
            } else {
                HeaderView(
                    title = model.address() ?: EMPTY_VALUE,
                    subtitle = null,
                    onInfoButton = null
                )
            }
        }

        // Update Daily Weather
        binding.dailyDate.text = forecast.date.getRelativeDayAndShort(this)
        binding.dailyIcon.setWeatherAnimation(forecast.icon)
        binding.dailyMaxTemp.text = getFormattedTemperature(this, forecast.maxTemp)
        binding.dailyMinTemp.text = getFormattedTemperature(this, forecast.minTemp)
        binding.precipProbabilityCard.setData(
            getFormattedPrecipitationProbability(forecast.precipProbability)
        )
        binding.windCard.setIcon(getWindDirectionDrawable(this, forecast.windDirection))
        binding.windCard.setData(getFormattedWind(this, forecast.windSpeed, forecast.windDirection))
        binding.dailyPrecipCard.setData(
            getFormattedPrecipitation(context = this, value = forecast.precip, isRainRate = false)
        )
        binding.uvCard.setData(getFormattedUV(this, forecast.uv))
        binding.humidityCard.setData(getFormattedHumidity(forecast.humidity))
        binding.pressureCard.setData(getFormattedPressure(this, forecast.pressure))

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
            visible(!charts.isEmpty())
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

    @Suppress("MagicNumber")
    private fun scrollToChart(chart: LineChartView) {
        val (chartX, chartY) = chart.screenLocation()
        val currentY = binding.mainContainer.scrollY

        /**
         * It didn't seem to scroll properly at the top of the chart's card,
         * as the title and the legends were cropped. So we subtract the custom value `110` here
         * at the end of the equation in order to fix this issue
         * and scroll properly to the top of the card containing the chart
         */
        val finalY = chartY - binding.appBar.height - binding.root.paddingTop + currentY - 110
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
                    visible(true)
                }
                DeviceRelation.FOLLOWED -> {
                    setImageResource(R.drawable.ic_favorite)
                    setColor(R.color.follow_heart_color)
                    visible(true)
                }
                else -> visible(false)
            }
        }
    }

    private fun handleSavedLocationIcon() {
        with(binding.locationStatusIcon) {
            if (model.location.isSaved) {
                setImageResource(R.drawable.ic_star_filled)
            } else {
                setImageResource(R.drawable.ic_star_outlined)
            }
            setColor(R.color.warning)
            visible(true)
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.DEVICE_FORECAST_DETAILS, classSimpleName())
    }
}
