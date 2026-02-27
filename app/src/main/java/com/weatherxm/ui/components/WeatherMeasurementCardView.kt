package com.weatherxm.ui.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.weatherxm.R
import com.weatherxm.databinding.ViewWeatherMeasurementCardBinding
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.compose.GradientIcon
import com.weatherxm.ui.components.compose.GradientIconRotatable

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

    fun setGradientIcon(iconRes: Int?, windDirection: Int?, isRotatableWindIcon: Boolean) {
        binding.gradientIcon.setContent {
            if (isRotatableWindIcon) {
                GradientIconRotatable(
                    iconRes = R.drawable.ic_wind_direction,
                    rotation = (windDirection?.toFloat() ?: 0f) + 180f,
                    size = 25.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            colorResource(R.color.blue),
                            colorResource(R.color.forecast_premium)
                        )
                    )
                )
            } else if(iconRes != null) {
                GradientIcon(
                    iconRes = iconRes,
                    size = 25.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            colorResource(R.color.blue),
                            colorResource(R.color.forecast_premium)
                        )
                    )
                )
            }
        }
        binding.icon.visible(false)
        binding.gradientIcon.visible(true)
    }

    fun setIcon(drawable: Drawable?) {
        drawable?.let {
            binding.icon.setImageDrawable(it)
            binding.gradientIcon.visible(false)
            binding.icon.visible(true)
        }
    }

    fun setData(value: String) {
        binding.value.text = value
    }
}
