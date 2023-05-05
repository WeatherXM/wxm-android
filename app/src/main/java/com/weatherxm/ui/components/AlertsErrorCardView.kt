package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import com.weatherxm.databinding.ViewAlertsErrorCardBinding

class AlertsErrorCardView : LinearLayout {

    private lateinit var binding: ViewAlertsErrorCardBinding

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
        binding = ViewAlertsErrorCardBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL
        gravity = Gravity.CENTER
    }

    fun title(@StringRes resId: Int): AlertsErrorCardView {
        title(resources.getString(resId))
        return this
    }

    fun title(subtitle: String?): AlertsErrorCardView {
        binding.title.apply {
            text = subtitle
            visibility = if (subtitle != null) View.VISIBLE else View.GONE
        }
        return this
    }

    fun action(listener: OnClickListener): AlertsErrorCardView {
        with(binding.action) {
            setOnClickListener(listener)
            visibility = VISIBLE
        }
        return this
    }
}
