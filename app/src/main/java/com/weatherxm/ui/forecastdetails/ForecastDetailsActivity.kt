package com.weatherxm.ui.forecastdetails

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityForecastDetailsBinding
import com.weatherxm.ui.common.Charts
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_FORECAST_SELECTED_DAY
import com.weatherxm.ui.common.Contracts.EMPTY_VALUE
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.HourlyForecastAdapter
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.ui.common.UIForecastDay
import com.weatherxm.ui.common.UILocation
import com.weatherxm.ui.common.capitalizeWords
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.screenLocation
import com.weatherxm.ui.common.setColor
import com.weatherxm.ui.common.setDisplayTimezone
import com.weatherxm.ui.common.setWeatherAnimation
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.components.LineChartView
import com.weatherxm.ui.components.compose.DailyTileForecast
import com.weatherxm.ui.components.compose.ForecastTabSelector
import com.weatherxm.ui.components.compose.HeaderView
import com.weatherxm.ui.components.compose.JoinNetworkPromoCard
import com.weatherxm.ui.components.compose.MosaicPromotionCard
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
            intent.parcelable<UILocation>(Contracts.ARG_LOCATION),
            intent.getBooleanExtra(Contracts.ARG_HAS_FREE_TRIAL_AVAILABLE, false)
        )
    }

    private lateinit var hourlyAdapter: HourlyForecastAdapter
    private var currentSelectedTab = 0

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
            initSavedLocationIcon()
            binding.displayTimeNotice.visible(false)
        }

        model.onDeviceDefaultForecast().observe(this) {
            if (currentSelectedTab == 0) {
                onForecast(it) { model.fetchDeviceForecasts() }
            }
        }

        model.onDevicePremiumForecast().observe(this) {
            if (currentSelectedTab == 1) {
                onForecast(it) { model.fetchDeviceForecasts() }
            }
        }

        model.onLocationForecast().observe(this) {
            onForecast(it) { model.fetchLocationForecast() }
        }

        if (!model.device.isEmpty()) {
            model.fetchDeviceForecasts()
            initForecastTabsSelector()
            initMosaicPromotionCard()
        } else if (!model.location.isEmpty()) {
            model.fetchLocationForecast()
        }
    }

    private fun initForecastTabsSelector() {
        binding.forecastTabSelector.setContent {
            ForecastTabSelector(0) { newSelectedTab ->
                currentSelectedTab = newSelectedTab
                if (newSelectedTab == 0) {
                    model.onDeviceDefaultForecast().value?.let {
                        onForecast(it) { model.fetchDeviceForecasts() }
                    }
                } else {
                    model.onDevicePremiumForecast().value?.let {
                        onForecast(it) { model.fetchDeviceForecasts() }
                    }
                }
            }
        }
    }

    private fun initMosaicPromotionCard() {
        binding.mosaicPromotionCard.setContent {
            MosaicPromotionCard(model.hasFreeTrialAvailable) {
                navigator.showManageSubscription(
                    this,
                    model.hasFreeTrialAvailable,
                    model.isLoggedIn()
                )
            }
        }
    }

    private fun onForecast(resource: Resource<UIForecast>, onErrorRetry: () -> Unit) {
        when (resource.status) {
            Status.SUCCESS -> {
                val forecast = resource.data ?: UIForecast.empty()
                val selectedDayPosition = model.getSelectedDayPosition(
                    intent.getStringExtra(ARG_FORECAST_SELECTED_DAY),
                    forecast
                )
                setupDailyAdapter(forecast, selectedDayPosition)
                updateUI(forecast, selectedDayPosition)
                binding.statusView.visible(false)
                binding.mainContainer.visible(true)
            }
            Status.ERROR -> {
                binding.statusView.clear()
                    .animation(R.raw.anim_error)
                    .title(getString(R.string.error_generic_message))
                    .subtitle(resource.message)
                    .action(getString(R.string.action_retry))
                    .listener { onErrorRetry.invoke() }
                binding.mainContainer.visible(false)
            }
            Status.LOADING -> {
                binding.statusView.clear().animation(R.raw.anim_loading)
                binding.mainContainer.visible(false)
                binding.statusView.visible(true)
            }
        }
    }

    @Suppress("LongMethod")
    private fun updateUI(forecast: UIForecast, selectedDayPosition: Int) {
        val forecastDay = forecast.forecastDays[selectedDayPosition]
        // Update the header now that model.address has valid data and we are in a location
        if (!model.location.isEmpty()) {
            binding.header.setContent {
                if (model.location.isCurrentLocation) {
                    HeaderView(
                        title = getString(R.string.current_location).capitalizeWords(),
                        subtitle = forecast.address,
                        onInfoButton = null
                    )
                } else {
                    HeaderView(
                        title = forecast.address ?: EMPTY_VALUE,
                        subtitle = null,
                        onInfoButton = null
                    )
                }
            }
        }

        // Update the "Powered By" card
        binding.poweredByMeteoblueIcon.visible(currentSelectedTab == 0)
        binding.poweredByWXMLogo.visible(currentSelectedTab == 1)

        // Update the forecast tabs or the mosaic prompt
        if (!model.device.isEmpty()) {
            binding.forecastTabSelector.visible(
                forecast.isPremium == true || currentSelectedTab == 1
            )
            binding.mosaicPromotionCard.visible(forecast.isPremium == false)
        }

        // Update Daily Weather
        binding.dailyDate.text = forecastDay.date.getRelativeDayAndShort(this)
        binding.dailyIcon.setWeatherAnimation(forecastDay.icon)
        binding.dailyMaxTemp.text = getFormattedTemperature(this, forecastDay.maxTemp)
        binding.dailyMinTemp.text = getFormattedTemperature(this, forecastDay.minTemp)

        /**
         * Some data are missing in the Hyper Local tab so we handle it differently below.
         */
        if (currentSelectedTab == 1) {
            binding.dailyPremiumWind.setGradientIcon(
                iconRes = null,
                windDirection = forecastDay.windDirection,
                isRotatableWindIcon = true
            )
            binding.dailyPremiumWind.setData(
                getFormattedWind(this, forecastDay.windSpeed, forecastDay.windDirection)
            )
            binding.dailyPremiumHumidity.setGradientIcon(
                iconRes = R.drawable.ic_weather_humidity,
                windDirection = null,
                isRotatableWindIcon = false
            )
            binding.dailyPremiumHumidity.setData(getFormattedHumidity(forecastDay.humidity))
            binding.dailyDefaultFirstRow.visible(false)
            binding.dailyDefaultSecondRow.visible(false)
            binding.dailyPremiumRow.visible(true)
        } else {
            binding.precipProbabilityCard.setData(
                getFormattedPrecipitationProbability(forecastDay.precipProbability)
            )
            binding.windCard.setIcon(getWindDirectionDrawable(this, forecastDay.windDirection))
            binding.humidityCard.setIcon(getDrawable(R.drawable.ic_weather_humidity))
            binding.windCard.setData(
                getFormattedWind(
                    this,
                    forecastDay.windSpeed,
                    forecastDay.windDirection
                )
            )
            binding.dailyPrecipCard.setData(
                getFormattedPrecipitation(
                    context = this,
                    value = forecastDay.precip,
                    isRainRate = false
                )
            )
            binding.uvCard.setData(getFormattedUV(this, forecastDay.uv))
            binding.humidityCard.setData(getFormattedHumidity(forecastDay.humidity))
            binding.pressureCard.setData(getFormattedPressure(this, forecastDay.pressure))
            binding.dailyPremiumRow.visible(false)
            binding.dailyDefaultFirstRow.visible(true)
            binding.dailyDefaultSecondRow.visible(true)
        }

        // Update Hourly Tiles
        hourlyAdapter = HourlyForecastAdapter(null)
        binding.hourlyForecastRecycler.adapter = hourlyAdapter
        hourlyAdapter.submitList(forecastDay.hourlyWeather)
        if (!forecastDay.hourlyWeather.isNullOrEmpty()) {
            binding.hourlyForecastRecycler.scrollToPosition(
                model.getDefaultHourPosition(forecastDay.hourlyWeather)
            )
        }

        // Update Charts
        updateCharts(forecast, forecastDay)
    }

    private fun updateCharts(forecast: UIForecast, forecastDay: UIForecastDay) {
        // Update Charts
        with(binding.charts) {
            val charts = model.getCharts(forecast, forecastDay)
            clearCharts()
            initTemperatureChart(charts.temperature, charts.feelsLike, true)
            initWindChart(charts.windSpeed, charts.windGust, charts.windDirection, true)
            initPrecipitationChart(
                charts.precipitation,
                charts.precipProbability,
                isHistoricalData = false,
                hideChartIfNoData = true
            )
            initHumidityChart(charts.humidity, true)
            initPressureChart(charts.pressure, true)
            initSolarChart(charts.uv, charts.solarRadiation, true)
            autoHighlightCharts(0F)
            setupChartsAndListeners(charts)
            visible(!charts.isEmpty())
        }
    }

    private fun setupChartsAndListeners(charts: Charts) {
        with(binding.charts) {
            chartPrecipitation().primaryLine(
                getString(R.string.precipitation), getString(R.string.precipitation)
            )
            if (charts.precipProbability.isDataValid()) {
                chartPrecipitation().secondaryLine(
                    getString(R.string.probability),
                    getString(R.string.precipitation_probability)
                )
            } else {
                chartPrecipitation().secondaryLine(null, null)
            }
            chartWind().primaryLine(null, getString(R.string.speed))
            chartWind().secondaryLine(null, null)
            chartSolar().updateTitle(getString(R.string.uv_index))
            chartSolar().primaryLine(null, getString(R.string.uv_index))
            chartSolar().secondaryLine(null, null)
            chartTemperature().updateIcon(
                R.drawable.ic_weather_temperature,
                currentSelectedTab == 1
            )
            chartPrecipitation().updateIcon(
                R.drawable.ic_weather_precipitation,
                currentSelectedTab == 1
            )
            chartWind().updateIcon(R.drawable.ic_weather_wind, currentSelectedTab == 1)
            chartHumidity().updateIcon(R.drawable.ic_weather_humidity, currentSelectedTab == 1)
            chartPressure().updateIcon(R.drawable.ic_weather_pressure, currentSelectedTab == 1)
            chartSolar().updateIcon(R.drawable.ic_weather_solar, currentSelectedTab == 1)
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
        val currentY = binding.scrollView.scrollY

        /**
         * It didn't seem to scroll properly at the top of the chart's card,
         * as the title and the legends were cropped. So we subtract the custom value `110` here
         * at the end of the equation in order to fix this issue
         * and scroll properly to the top of the card containing the chart
         */
        val finalY = chartY - binding.appBar.height - binding.root.paddingTop + currentY - 110
        binding.scrollView.smoothScrollTo(chartX, finalY, SCROLL_DURATION_MS)
    }

    private fun setupDailyAdapter(forecast: UIForecast, selectedDayPosition: Int) {
        binding.dailyTilesCompose.setContent {
            DailyTileForecast(
                forecastDays = forecast.forecastDays,
                selectedDate = forecast.forecastDays[selectedDayPosition].date,
                isPremiumTabSelected = currentSelectedTab == 1,
                onDaySelected = { selectedDate ->
                    analytics.trackEventSelectContent(
                        AnalyticsService.ParamValue.DAILY_CARD.paramValue,
                        Pair(
                            FirebaseAnalytics.Param.ITEM_ID,
                            AnalyticsService.ParamValue.DAILY_DETAILS.paramValue
                        )
                    )
                    val newSelectedDayPosition = forecast.forecastDays.indexOfFirst {
                        it.date == selectedDate
                    }
                    if (newSelectedDayPosition != -1) {
                        updateUI(forecast, newSelectedDayPosition)
                    }
                }
            )
        }
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

    private fun initSavedLocationIcon() {
        if (model.location.isSaved) {
            binding.locationStatusBtn.setOnClickListener {
                setResult(RESULT_OK)
                model.removeSavedLocation()
                initSavedLocationIcon()
            }
            binding.locationStatusBtn.setImageResource(R.drawable.ic_star_filled)
        } else {
            binding.locationStatusBtn.setOnClickListener {
                if (model.canSaveMoreLocations()) {
                    /**
                     * Set result to OK so that the previous screen (locations in home) gets
                     * triggered for an update
                     */
                    setResult(RESULT_OK)

                    model.addSavedLocation()
                    val stateParam = if (model.isLoggedIn()) {
                        AnalyticsService.ParamValue.AUTHENTICATED.paramValue
                    } else {
                        AnalyticsService.ParamValue.UNAUTHENTICATED.paramValue
                    }
                    analytics.trackEventUserAction(
                        actionName = AnalyticsService.ParamValue.SAVED_A_LOCATION.paramValue,
                        contentType = null,
                        Pair(
                            AnalyticsService.CustomParam.STATE.paramName,
                            stateParam
                        )
                    )

                    initSavedLocationIcon()
                } else if (model.isLoggedIn()) {
                    toast(R.string.maxed_out_saved_locations)
                    analytics.trackEventViewContent(
                        AnalyticsService.ParamValue.MAX_LOCATIONS_SAVED_ERROR.paramValue,
                        Pair(
                            AnalyticsService.CustomParam.STATE.paramName,
                            AnalyticsService.ParamValue.AUTHENTICATED.paramValue
                        )
                    )
                } else {
                    analytics.trackEventViewContent(
                        AnalyticsService.ParamValue.MAX_LOCATIONS_SAVED_ERROR.paramValue,
                        Pair(
                            AnalyticsService.CustomParam.STATE.paramName,
                            AnalyticsService.ParamValue.UNAUTHENTICATED.paramValue
                        )
                    )
                    navigator.showLoginDialog(
                        fragmentActivity = this@ForecastDetailsActivity,
                        title = getString(R.string.save_more_locations),
                        message = getString(R.string.maxed_out_saved_locations_sign_in)
                    )
                }
            }
            binding.locationStatusBtn.setImageResource(R.drawable.ic_star_outlined)
        }
        binding.locationStatusBtn.setColor(R.color.warning)
        binding.locationStatusBtn.visible(true)
    }

    override fun onResume() {
        super.onResume()
        if (!model.device.isEmpty()) {
            analytics.trackScreen(
                AnalyticsService.Screen.DEVICE_FORECAST_DETAILS,
                classSimpleName()
            )
        } else {
            analytics.trackScreen(
                screen = AnalyticsService.Screen.LOCATION_FORECAST_DETAILS,
                screenClass = classSimpleName(),
                itemId = if (model.location.isSaved) {
                    AnalyticsService.ParamValue.SAVED_LOCATION.paramValue
                } else {
                    AnalyticsService.ParamValue.UNSAVED_LOCATION.paramValue
                }
            )
        }

        if (!model.isLoggedIn()) {
            binding.joinNetworkCard.setContent {
                JoinNetworkPromoCard {
                    navigator.openWebsite(this, getString(R.string.shop_url))
                }
            }
        }
        binding.joinNetworkCard.visible(model.isLoggedIn())
    }
}
