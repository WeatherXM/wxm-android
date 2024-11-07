package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.weatherxm.R
import com.weatherxm.data.models.HourlyWeather
import com.weatherxm.data.services.CacheService.Companion.KEY_PRESSURE
import com.weatherxm.data.services.CacheService.Companion.KEY_TEMPERATURE
import com.weatherxm.data.services.CacheService.Companion.KEY_WIND
import com.weatherxm.databinding.ViewWeatherCardBinding
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.setNoDataMessage
import com.weatherxm.ui.common.setWeatherAnimation
import com.weatherxm.ui.common.visible
import com.weatherxm.util.DateTimeHelper.getFormattedDateAndTime
import com.weatherxm.util.Weather
import com.weatherxm.util.Weather.getFormattedHumidity
import com.weatherxm.util.Weather.getPrecipitationPreferredUnit
import com.weatherxm.util.Weather.getUVClassification
import com.weatherxm.util.Weather.getWindDirectionDrawable

class WeatherCardView : LinearLayout {

    private lateinit var binding: ViewWeatherCardBinding
    private var weatherData: HourlyWeather? = null

    constructor(context: Context?) : super(context) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context?) {
        binding = ViewWeatherCardBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL
        gravity = Gravity.CENTER
    }

    private fun updateCurrentWeatherUI() {
        with(binding) {
            icon.setWeatherAnimation(weatherData?.icon)
            val temperatureUnitText = Weather.getPreferredUnit(
                context.getString(KEY_TEMPERATURE), context.getString(R.string.temperature_celsius)
            )
            temperature.text = Weather.getFormattedTemperature(
                weatherData?.temperature, 1, includeUnit = false
            )
            temperatureUnit.text = temperatureUnitText
            feelsLike.text = Weather.getFormattedTemperature(
                weatherData?.feelsLike, 1, includeUnit = false
            )
            feelsLikeUnit.text = temperatureUnitText
            dewPoint.setData(
                Weather.getFormattedTemperature(weatherData?.dewPoint, 1, includeUnit = false),
                temperatureUnitText
            )

            humidity.setData(getFormattedHumidity(weatherData?.humidity, includeUnit = false), "%")

            val windValue = Weather.getFormattedWind(
                weatherData?.windSpeed, weatherData?.windDirection, includeUnits = false
            )
            val windUnit = Weather.getPreferredUnit(
                context.getString(KEY_WIND), context.getString(R.string.wind_speed_ms)
            )
            val windDirectionUnit = Weather.getFormattedWindDirection(weatherData?.windDirection)
            val windGustValue = Weather.getFormattedWind(
                weatherData?.windGust, weatherData?.windDirection, includeUnits = false
            )
            val windDirDrawable = getWindDirectionDrawable(context, weatherData?.windDirection)
            wind.setData(windValue, "$windUnit $windDirectionUnit", windDirDrawable)
            windGust.setData(windGustValue, "$windUnit $windDirectionUnit", windDirDrawable)

            rainRate.setData(
                Weather.getFormattedPrecipitation(weatherData?.precipitation, includeUnit = false),
                getPrecipitationPreferredUnit(true)
            )
            precipitationAccumulated.setData(
                Weather.getFormattedPrecipitation(
                    weatherData?.precipAccumulated,
                    isRainRate = false,
                    includeUnit = false
                ),
                getPrecipitationPreferredUnit(false)
            )

            val pressureUnit = Weather.getPreferredUnit(
                context.getString(KEY_PRESSURE), context.getString(R.string.pressure_hpa)
            )
            pressure.setData(
                Weather.getFormattedPressure(weatherData?.pressure, includeUnit = false),
                pressureUnit
            )
            uv.setData(
                Weather.getFormattedUV(weatherData?.uvIndex, includeUnit = false),
                getUVClassification(weatherData?.uvIndex)
            )
            solarRadiation.setData(
                Weather.getFormattedSolarRadiation(weatherData?.solarIrradiance, false),
                context.getString(R.string.solar_radiation_unit)
            )
        }
    }

    fun setData(device: UIDevice) {
        val data = device.currentWeather
        if (data == null || data.isEmpty()) {
            binding.weatherDataLayout.visible(false)
            binding.secondaryCard.visible(false)
            binding.noDataLayout.visible(true)
            device.setNoDataMessage(context, binding.noDataMessage)
        } else {
            weatherData = data
            updateCurrentWeatherUI()
            binding.lastUpdatedOn.text = context.getString(
                R.string.last_updated,
                data.timestamp.getFormattedDateAndTime(context)
            )
        }

    }
}
