package com.weatherxm.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import com.weatherxm.R
import com.weatherxm.databinding.ViewErrorCardBinding
import com.weatherxm.util.setHtml

class ErrorCardView : LinearLayout {

    private lateinit var binding: ViewErrorCardBinding

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
        binding = ViewErrorCardBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL
        gravity = Gravity.CENTER

        binding.closeButton.setOnClickListener {
            hide()
        }

        this.context.theme.obtainStyledAttributes(attrs, R.styleable.ErrorCardView, 0, 0).apply {
            try {
                title(getString(R.styleable.ErrorCardView_error_title))
                message(getString(R.styleable.ErrorCardView_error_message))
            } finally {
                recycle()
            }
        }
    }

    fun title(@StringRes resId: Int): ErrorCardView {
        title(resources.getString(resId))
        return this
    }

    fun title(subtitle: String?): ErrorCardView {
        binding.title.apply {
            text = subtitle
            visibility = if (subtitle != null) View.VISIBLE else View.GONE
        }
        return this
    }

    fun message(@StringRes resId: Int): ErrorCardView {
        message(resources.getString(resId))
        return this
    }

    fun message(subtitle: String?): ErrorCardView {
        binding.message.apply {
            text = subtitle
            visibility = if (subtitle != null) View.VISIBLE else View.GONE
        }
        return this
    }

    fun htmlMessage(@StringRes resId: Int, arg: String? = null): ErrorCardView {
        binding.message.apply {
            if (arg.isNullOrEmpty()) {
                setHtml(resId)
            } else {
                setHtml(resId, arg)
            }
            visibility = View.VISIBLE
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
