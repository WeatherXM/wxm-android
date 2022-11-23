package com.weatherxm.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.StringRes
import com.weatherxm.R
import com.weatherxm.databinding.ViewWeatherOverlayErrorCardBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.util.setHtml
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WeatherOverlayErrorCardView : LinearLayout, KoinComponent {
    private lateinit var binding: ViewWeatherOverlayErrorCardBinding
    private val navigator: Navigator by inject()

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
        binding = ViewWeatherOverlayErrorCardBinding.inflate(LayoutInflater.from(context), this)
        binding.closeButton.setOnClickListener {
            hide()
        }
        orientation = VERTICAL
        gravity = Gravity.CENTER
    }

    fun setErrorMessageWithUrl(@StringRes errorMessageResId: Int) {
        with(binding.errorText) {
            movementMethod = BetterLinkMovementMethod.newInstance().apply {
                setOnLinkClickListener { _, url ->
                    navigator.openWebsite(context, url)
                    return@setOnLinkClickListener true
                }
            }
            setHtml(errorMessageResId, context.resources.getString(R.string.troubleshooting_url))
        }
        show()
    }

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }
}
