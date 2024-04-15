package com.weatherxm.ui.deviceforecast

import android.os.Bundle
import com.weatherxm.R
import com.weatherxm.data.HourlyWeather
import com.weatherxm.databinding.ActivityForecastDetailsBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_FORECAST_SELECTED_DAY
import com.weatherxm.ui.common.Contracts.ARG_FORECAST_SELECTED_HOUR
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.HourlyForecastAdapter
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.ui.common.UIForecastDay
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.setColor
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.util.Analytics
import com.weatherxm.util.DateTimeHelper.getRelativeDayAndShort
import com.weatherxm.util.Weather
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
        setupHourlyAdapter(forecastDay, selectedHour)
        updateDailyWeather(forecastDay)
    }

    private fun setupDailyAdapter(forecastDay: UIForecastDay, selectedDayPosition: Int) {
        dailyAdapter = DailyTileForecastAdapter(forecastDay.date) {
            // Get selected position before we change it to the new one in order to reset the stroke
            dailyAdapter.notifyItemChanged(dailyAdapter.getSelectedPosition())
            setupHourlyAdapter(it, null)
            updateDailyWeather(it)
        }
        binding.dailyTilesRecycler.adapter = dailyAdapter
        dailyAdapter.submitList(model.forecast.forecastDays)
        binding.dailyTilesRecycler.scrollToPosition(selectedDayPosition)
    }

    private fun setupHourlyAdapter(forecastDay: UIForecastDay, selectedHour: HourlyWeather?) {
        val hourlyAdapter = HourlyForecastAdapter(null)
        binding.hourlyForecastRecycler.adapter = hourlyAdapter
        hourlyAdapter.submitList(forecastDay.hourlyWeather)
        if (!forecastDay.hourlyWeather.isNullOrEmpty()) {
            binding.hourlyForecastRecycler.scrollToPosition(
                model.getSelectedHourPosition(forecastDay.hourlyWeather, selectedHour)
            )
        }
    }

    private fun updateDailyWeather(forecast: UIForecastDay) {
        binding.dailyDate.text = forecast.date.getRelativeDayAndShort(this)
        binding.dailyIcon.apply {
            setAnimation(Weather.getWeatherAnimation(forecast.icon))
            playAnimation()
        }
        binding.dailyMaxTemp.text = Weather.getFormattedTemperature(forecast.maxTemp, 1)
        binding.dailyMinTemp.text = Weather.getFormattedTemperature(forecast.minTemp, 1)
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
        analytics.trackScreen(Analytics.Screen.DEVICE_FORECAST_DETAILS, this::class.simpleName)
    }
}
