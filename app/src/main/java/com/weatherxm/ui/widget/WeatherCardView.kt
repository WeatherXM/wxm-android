package com.weatherxm.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.weatherxm.R
import com.weatherxm.data.HourlyWeather
import com.weatherxm.databinding.ViewWeatherCardBinding
import com.weatherxm.util.DateTimeHelper.getFormattedDay
import com.weatherxm.util.DateTimeHelper.getFormattedTime
import com.weatherxm.util.Weather

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

    fun updateCurrentWeatherUI(tz: String?, decimalsOnTemp: Int = 1) {
        with(binding) {
            icon.setAnimation(Weather.getWeatherAnimation(weatherData?.icon))
            icon.playAnimation()
            temperature.text =
                Weather.getFormattedTemperature(weatherData?.temperature, decimalsOnTemp)
            feelsLike.text = Weather.getFormattedTemperature(weatherData?.feelsLike, decimalsOnTemp)
            rainRate.text = Weather.getFormattedPrecipitation(weatherData?.precipitation)
            pressure.text = Weather.getFormattedPressure(weatherData?.pressure)
            humidity.text = Weather.getFormattedHumidity(weatherData?.humidity)
            wind.text = Weather.getFormattedWind(weatherData?.windSpeed, weatherData?.windDirection)
            solar.text = Weather.getFormattedUV(weatherData?.uvIndex)
            updatedOn.text = weatherData?.timestamp?.let {
                val day = it.getFormattedDay(context, true)
                val time = it.getFormattedTime(context)
                "$day, $time"
            } ?: ""

            with(timezoneInfo) {
                visibility = tz?.let {
                    text = context.getString(R.string.displayed_times, it)
                    android.view.View.VISIBLE
                } ?: android.view.View.GONE
            }


        }
    }

    fun setData(data: HourlyWeather?, tz: String?, decimalsOnTemp: Int = 1) {
        weatherData = data
        updateCurrentWeatherUI(tz, decimalsOnTemp)
    }

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }
}
