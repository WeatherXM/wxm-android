package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.weatherxm.R
import com.weatherxm.databinding.ViewWeatherDataBinding

class WeatherDataView : LinearLayout {

    private lateinit var binding: ViewWeatherDataBinding

    constructor(context: Context?) : super(context) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context?, attrs: AttributeSet? = null) {
        binding = ViewWeatherDataBinding.inflate(LayoutInflater.from(context), this)

        this.context.theme.obtainStyledAttributes(attrs, R.styleable.WeatherDataView, 0, 0).apply {
            try {
                binding.title.text = getString(R.styleable.WeatherDataView_weather_data_title)
                binding.icon.setImageDrawable(
                    getDrawable(R.styleable.WeatherDataView_weather_data_icon)
                )
                val description = getString(R.styleable.WeatherDataView_weather_data_description)
                description?.let {
                    binding.description.text = it
                    binding.description.visibility = VISIBLE
                }
            } finally {
                recycle()
            }
        }
    }

    fun setData(value: String, unit: String? = null) {
        binding.value.text = value
        binding.unit.text = unit
    }
}
