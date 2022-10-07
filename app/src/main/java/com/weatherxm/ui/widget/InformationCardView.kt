package com.weatherxm.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import com.weatherxm.R
import com.weatherxm.databinding.ViewInformationCardBinding

class InformationCardView : LinearLayout {

    private lateinit var binding: ViewInformationCardBinding

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

        this.context.theme.obtainStyledAttributes(attrs, R.styleable.InformationCardView, 0, 0)
            .apply {
                try {
                    message(getString(R.styleable.InformationCardView_info_message))
                } finally {
                    recycle()
                }
            }
    }

    private fun init(context: Context?, attrs: AttributeSet? = null) {
        binding = ViewInformationCardBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL
        gravity = Gravity.CENTER
    }

    fun message(@StringRes resId: Int): InformationCardView {
        message(resources.getString(resId))
        return this
    }

    fun message(subtitle: String?): InformationCardView {
        binding.message.apply {
            text = subtitle
            visibility = if (subtitle != null) View.VISIBLE else View.GONE
        }
        return this
    }

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }
}
