package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.weatherxm.databinding.ViewWeatherLocationBinding
import com.weatherxm.ui.common.setWeatherAnimation
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

    fun setData() {
        binding.icon.setWeatherAnimation("partly-cloudy-day")
        binding.locationName.text = "Thessaloniki, GR"
        binding.locationCondition.text = "Partly Cloudy"
        binding.currentTemperature.text = getFormattedTemperature(context, 15F, fullUnit = false)
        binding.dailyMinTemp.text = getFormattedTemperature(context, 10F, fullUnit = false)
        binding.dailyMaxTemp.text = getFormattedTemperature(context, 20F, fullUnit = false)
    }
}
