package com.weatherxm.ui.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.weatherxm.R
import com.weatherxm.databinding.ViewWeatherMeasurementCardBinding

class WeatherMeasurementCardView : LinearLayout {

    private lateinit var binding: ViewWeatherMeasurementCardBinding

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
        binding = ViewWeatherMeasurementCardBinding.inflate(LayoutInflater.from(context), this)

        this.context.theme.obtainStyledAttributes(attrs, R.styleable.WeatherDataView, 0, 0).apply {
            try {
                binding.title.text = getString(R.styleable.WeatherDataView_weather_data_title)
                binding.icon.setImageDrawable(
                    getDrawable(R.styleable.WeatherDataView_weather_data_icon)
                )
            } finally {
                recycle()
            }
        }
    }

    fun setIcon(drawable: Drawable?) {
        drawable?.let {
            binding.icon.setImageDrawable(it)
        }
    }

    fun setData(
        value: String,
        unit: String? = null,
        spannableStringBuilder: SpannableStringBuilder? = null
    ) {
        binding.value.text = value
        unit?.let {
            binding.unit.text = it
        }
        spannableStringBuilder?.let {
            binding.unit.text = it
        }
    }
}
