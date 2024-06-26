package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import com.weatherxm.databinding.ViewMultipleAlertsCardBinding
import com.weatherxm.ui.common.visible

class MultipleAlertsCardView : LinearLayout {

    private lateinit var binding: ViewMultipleAlertsCardBinding

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
        binding = ViewMultipleAlertsCardBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL
        gravity = Gravity.CENTER
    }

    fun title(subtitle: String?): MultipleAlertsCardView {
        binding.title.apply {
            text = subtitle
            visible(subtitle != null)
        }
        return this
    }

    fun action(listener: OnClickListener): MultipleAlertsCardView {
        with(binding.action) {
            setOnClickListener(listener)
            visible(true)
        }
        return this
    }

    fun setIcon(@DrawableRes drawableResId: Int): MultipleAlertsCardView {
        binding.icon.setImageDrawable(
            ResourcesCompat.getDrawable(resources, drawableResId, context.theme)
        )
        binding.icon.visible(true)
        return this
    }

    fun setBackground(@ColorRes colorResId: Int): MultipleAlertsCardView {
        binding.card.setCardBackgroundColor(context.getColor(colorResId))
        return this
    }
}
