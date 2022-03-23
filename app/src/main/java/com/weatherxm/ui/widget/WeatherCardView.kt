package com.weatherxm.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.weatherxm.data.HourlyWeather
import com.weatherxm.databinding.ViewWeatherCardBinding
import com.weatherxm.util.ResourcesHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WeatherCardView : LinearLayout, KoinComponent {

    private lateinit var binding: ViewWeatherCardBinding
    private val resHelper: ResourcesHelper by inject()
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

    private fun updateCurrentWeatherUI(decimalsOnTemp: Int) {
        with(binding) {
            icon.setAnimation(Weather.getWeatherAnimation(weatherData?.icon))
            icon.playAnimation()
            temperature.text =
                Weather.getFormattedTemperature(weatherData?.temperature, decimalsOnTemp)
            precipitationIntensity.text =
                Weather.getFormattedPrecipitation(weatherData?.precipitation)
            pressure.text = Weather.getFormattedPressure(weatherData?.pressure)
            humidity.text = Weather.getFormattedHumidity(weatherData?.humidity)
            wind.text = Weather.getFormattedWind(weatherData?.windSpeed, weatherData?.windDirection)
            cloud.text = Weather.getFormattedCloud(weatherData?.cloudCover)
            solar.text = Weather.getFormattedUV(weatherData?.uvIndex)
            updatedOn.text = weatherData?.timestamp?.let {
                val day = getRelativeDayFromISO(resHelper, it, includeDate = true, fullName = true)
                val time = getHourMinutesFromISO(context, it)
                "$day, $time"
            } ?: ""
        }
    }

    fun setWeatherData(data: HourlyWeather?, decimalsOnTemp: Int = 0) {
        weatherData = data
        updateCurrentWeatherUI(decimalsOnTemp)
    }

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }
}
