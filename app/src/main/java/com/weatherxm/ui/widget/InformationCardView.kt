package com.weatherxm.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.StringRes
import com.weatherxm.R
import com.weatherxm.databinding.ViewInformationCardBinding
import com.weatherxm.util.setHtml
import me.saket.bettermovementmethod.BetterLinkMovementMethod

class InformationCardView : FrameLayout {

    private lateinit var binding: ViewInformationCardBinding

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(
        context: Context?,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) {
        binding = ViewInformationCardBinding.inflate(LayoutInflater.from(context), this)

        this.context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.InformationCardView,
            defStyleAttr,
            R.style.Widget_WeatherXM_MaterialCard_Info
        ).apply {
            try {
                val text = getString(R.styleable.InformationCardView_info_message)
                val htmlText = getString(R.styleable.InformationCardView_info_html_message)
                if (text != null) {
                    message(text)
                } else if (htmlText != null) {
                    htmlMessage(htmlText)
                }
                if (!getBoolean(R.styleable.InformationCardView_info_includes_stroke, false)) {
                    binding.card.strokeWidth = 0
                }
            } finally {
                recycle()
            }
        }
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

    fun htmlMessage(
        @StringRes resId: Int,
        arg: String? = null,
        linkClickedListener: (() -> Unit)? = null
    ): InformationCardView {
        binding.message.apply {
            if (arg.isNullOrEmpty()) {
                setHtml(resId)
            } else {
                setHtml(resId, arg)
            }
            if (linkClickedListener != null) {
                movementMethod = BetterLinkMovementMethod.newInstance().apply {
                    setOnLinkClickListener { _, _ ->
                        linkClickedListener.invoke()
                        return@setOnLinkClickListener true
                    }
                }
            }
            visibility = View.VISIBLE
        }
        return this
    }

    private fun htmlMessage(
        message: String,
        arg: String? = null,
        linkClickedListener: (() -> Unit)? = null
    ): InformationCardView {
        binding.message.apply {
            if (arg.isNullOrEmpty()) {
                setHtml(message)
            } else {
                setHtml(message, arg)
            }
            if (linkClickedListener != null) {
                movementMethod = BetterLinkMovementMethod.newInstance().apply {
                    setOnLinkClickListener { _, _ ->
                        linkClickedListener.invoke()
                        return@setOnLinkClickListener true
                    }
                }
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
