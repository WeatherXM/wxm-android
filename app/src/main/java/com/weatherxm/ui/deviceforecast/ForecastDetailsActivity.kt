package com.weatherxm.ui.deviceforecast

import android.graphics.Typeface
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import com.weatherxm.R
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.services.CacheService.Companion.KEY_PRESSURE
import com.weatherxm.data.services.CacheService.Companion.KEY_WIND
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
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.setColor
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.util.Analytics
import com.weatherxm.util.DateTimeHelper.getRelativeDayAndShort
import com.weatherxm.util.UnitConverter
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
import com.weatherxm.util.Weather.getWeatherAnimation
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
            setAnimation(getWeatherAnimation(forecast.icon))
            playAnimation()
        }
        binding.dailyMaxTemp.text = getFormattedTemperature(forecast.maxTemp, 1)
        binding.dailyMinTemp.text = getFormattedTemperature(forecast.minTemp, 1)

        binding.precipProbabilityCard.setData(
            getFormattedPrecipitationProbability(forecast.precipProbability, false), "%"
        )
        val windValue =
            getFormattedWind(forecast.windSpeed, forecast.windDirection, includeUnits = false)
        val windUnit = getPreferredUnit(getString(KEY_WIND), getString(R.string.wind_speed_ms))
        val windDirectionUnit = forecast.windDirection?.let {
            getFormattedWindDirection(it)
        } ?: String.empty()
        val windUnitWithDirection = "$windUnit $windDirectionUnit"
        val formattedWindUnit = SpannableStringBuilder(windUnitWithDirection)
        val boldToStart = windUnitWithDirection.indexOf(windDirectionUnit, ignoreCase = true)
        formattedWindUnit.setSpan(
            StyleSpan(Typeface.BOLD),
            boldToStart,
            boldToStart.plus(windDirectionUnit.length),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val windDrawable = forecast.windDirection?.let {
            val windDirectionDrawable = ResourcesCompat.getDrawable(
                resources, R.drawable.layers_wind_direction, null
            ) as LayerDrawable
            windDirectionDrawable.getDrawable(UnitConverter.getIndexOfCardinal(it))
        } ?: AppCompatResources.getDrawable(this, R.drawable.ic_weather_wind)
        binding.windCard.setIcon(windDrawable)
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

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(Analytics.Screen.DEVICE_FORECAST_DETAILS, this::class.simpleName)
    }
}
