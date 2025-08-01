package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.weatherxm.databinding.ViewWeatherLocationBinding
import com.weatherxm.ui.common.LocationWeather
import com.weatherxm.ui.common.setWeatherAnimation
import com.weatherxm.ui.common.visible
import com.weatherxm.util.Weather.getFormattedTemperature

class WeatherLocationCardView : LinearLayout {

    private lateinit var binding: ViewWeatherLocationBinding

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
        binding = ViewWeatherLocationBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL
        gravity = Gravity.CENTER
    }

    fun setData(weather: LocationWeather, listener: () -> Unit) {
        binding.root.setOnClickListener { listener() }
        binding.icon.setWeatherAnimation(weather.icon)
        binding.locationName.text = weather.address
        weather.currentWeatherSummaryResId?.let {
            binding.locationSummaryCondition.text = context.getString(it)
        } ?: binding.locationSummaryCondition.visible(false)
        binding.currentTemperature.text =
            getFormattedTemperature(context, weather.currentTemp, fullUnit = false)
        binding.dailyMinTemp.text =
            getFormattedTemperature(context, weather.dailyMinTemp, fullUnit = false)
        binding.dailyMaxTemp.text =
            getFormattedTemperature(context, weather.dailyMaxTemp, fullUnit = false)
    }
}
