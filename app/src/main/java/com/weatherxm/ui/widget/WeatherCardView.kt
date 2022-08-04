package com.weatherxm.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.weatherxm.R
import com.weatherxm.data.HourlyWeather
import com.weatherxm.databinding.ViewWeatherCardBinding
import com.weatherxm.util.DateTimeHelper.getHourMinutesFromISO
import com.weatherxm.util.DateTimeHelper.getRelativeDayFromISO
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.Weather
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

    fun updateCurrentWeatherUI(decimalsOnTemp: Int, tz: String?) {
        with(binding) {
            icon.setAnimation(Weather.getWeatherAnimation(weatherData?.icon))
            icon.playAnimation()
            temperature.text =
                Weather.getFormattedTemperature(weatherData?.temperature, decimalsOnTemp)
            feelsLike.text = Weather.getFormattedTemperature(weatherData?.feelsLike, decimalsOnTemp)
            precipitationIntensity.text =
                Weather.getFormattedPrecipitation(weatherData?.precipitation)
            pressure.text = Weather.getFormattedPressure(weatherData?.pressure)
            humidity.text = Weather.getFormattedHumidity(weatherData?.humidity)
            wind.text = Weather.getFormattedWind(weatherData?.windSpeed, weatherData?.windDirection)
            solar.text = Weather.getFormattedUV(weatherData?.uvIndex)
            updatedOn.text = weatherData?.timestamp?.let {
                val day = getRelativeDayFromISO(resHelper, it, true)
                val time = getHourMinutesFromISO(context, it)
                "$day, $time"
            } ?: ""

            with(timezoneInfo) {
                visibility = tz?.let {
                    text = resHelper.getString(R.string.displayed_times, it)
                    android.view.View.VISIBLE
                } ?: android.view.View.GONE
            }


        }
    }

    fun setData(data: HourlyWeather?, tz: String?, decimalsOnTemp: Int = 0) {
        weatherData = data
        updateCurrentWeatherUI(decimalsOnTemp, tz)
    }

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }
}
