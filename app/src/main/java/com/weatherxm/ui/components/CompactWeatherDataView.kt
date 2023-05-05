package com.weatherxm.ui.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.weatherxm.R
import com.weatherxm.databinding.ViewCompactWeatherDataBinding

class CompactWeatherDataView : ConstraintLayout {

    private lateinit var binding: ViewCompactWeatherDataBinding

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet? = null) {
        binding = ViewCompactWeatherDataBinding.inflate(LayoutInflater.from(context), this, true)

        this.context.theme.obtainStyledAttributes(attrs, R.styleable.CompactWeatherDataView, 0, 0)
            .apply {
                try {
                    binding.icon.setImageDrawable(
                        getDrawable(R.styleable.CompactWeatherDataView_compact_weather_data_icon)
                    )
                } finally {
                    recycle()
                }
            }
    }

    fun setData(value: String, unit: String? = null, drawable: Drawable? = null) {
        binding.value.text = value
        binding.unit.text = unit

        drawable?.let {
            binding.icon.setImageDrawable(it)
        }
    }
}
