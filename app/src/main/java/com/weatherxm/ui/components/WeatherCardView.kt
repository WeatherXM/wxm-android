package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.weatherxm.R
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.services.CacheService.Companion.KEY_PRESSURE
import com.weatherxm.data.services.CacheService.Companion.KEY_TEMPERATURE
import com.weatherxm.data.services.CacheService.Companion.KEY_WIND
import com.weatherxm.databinding.ViewWeatherCardBinding
import com.weatherxm.util.Weather
import com.weatherxm.util.Weather.getPrecipitationPreferredUnit
import org.koin.core.component.KoinComponent

class WeatherCardView : LinearLayout, KoinComponent {

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

    fun updateCurrentWeatherUI() {
        with(binding) {
            icon.setAnimation(Weather.getWeatherAnimation(weatherData?.icon))
            icon.playAnimation()

            val temperatureUnit = Weather.getPreferredUnit(
                context.getString(KEY_TEMPERATURE), context.getString(R.string.temperature_celsius)
            )
            temperature.text = Weather.getFormattedTemperature(
                weatherData?.temperature, 1, includeUnit = false
            )
            binding.temperatureUnit.text = temperatureUnit
            binding.feelsLike.text = Weather.getFormattedTemperature(
                weatherData?.feelsLike, 1, includeUnit = false
            )
            binding.feelsLikeUnit.text = temperatureUnit
            binding.dewPoint.setData(
                Weather.getFormattedTemperature(weatherData?.dewPoint, 1, includeUnit = false),
                temperatureUnit
            )

            binding.humidity.setData(
                Weather.getFormattedHumidity(weatherData?.humidity, includeUnit = false), "%"
            )

            val windValue = Weather.getFormattedWind(
                weatherData?.windSpeed, weatherData?.windDirection, includeUnits = false
            )
            val windUnit = Weather.getPreferredUnit(
                context.getString(KEY_WIND), context.getString(R.string.wind_speed_ms)
            )
            val windDirectionUnit = weatherData?.windDirection?.let {
                Weather.getFormattedWindDirection(it)
            } ?: ""
            val windGustValue = Weather.getFormattedWind(
                weatherData?.windGust, weatherData?.windDirection, includeUnits = false
            )
            binding.wind.setData(windValue, "$windUnit $windDirectionUnit")
            binding.windGust.setData(windGustValue, "$windUnit $windDirectionUnit")

            binding.rainRate.setData(
                Weather.getFormattedPrecipitation(weatherData?.precipitation, includeUnit = false),
                getPrecipitationPreferredUnit(true)
            )
            binding.precipitationAccumulated.setData(
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
            binding.pressure.setData(
                Weather.getFormattedPressure(weatherData?.pressure, includeUnit = false),
                pressureUnit
            )
            binding.uv.setData(Weather.getFormattedUV(weatherData?.uvIndex))
            binding.solarRadiation.setData(
                Weather.getFormattedSolarRadiation(weatherData?.solarIrradiance, false),
                context.getString(R.string.solar_radiation_unit)
            )
        }
    }

    fun setData(data: HourlyWeather?) {
        weatherData = data
        updateCurrentWeatherUI()
    }
}
