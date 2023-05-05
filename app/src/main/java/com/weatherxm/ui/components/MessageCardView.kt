package com.weatherxm.ui.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import com.google.android.material.button.MaterialButton.ICON_GRAVITY_END
import com.weatherxm.R
import com.weatherxm.databinding.ViewMessageCardBinding
import com.weatherxm.ui.common.hide
import com.weatherxm.util.setColor
import me.saket.bettermovementmethod.BetterLinkMovementMethod

class MessageCardView : LinearLayout {

    private lateinit var binding: ViewMessageCardBinding

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
        binding = ViewMessageCardBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL
        gravity = Gravity.CENTER

        binding.closeButton.setOnClickListener {
            hide()
        }

        val attributes =
            this.context.theme.obtainStyledAttributes(attrs, R.styleable.MessageCardView, 0, 0)

        try {
            attributes.getString(R.styleable.MessageCardView_message_title)?.let {
                title(it)
            } ?: binding.title.hide(null)

            val text = attributes.getString(R.styleable.MessageCardView_message_message)
            val htmlText = attributes.getString(R.styleable.MessageCardView_message_html_message)
            if (text != null) {
                message(text)
            } else if (htmlText != null) {
                htmlMessage(htmlText)
            } else {
                binding.message.hide(null)
            }

            attributes.getBoolean(R.styleable.MessageCardView_message_includes_close_button, true)
                .apply {
                    if (this) {
                        binding.closeButton.visibility = View.VISIBLE
                    } else {
                        binding.closeButton.visibility = View.GONE
                    }
                }

            attributes.getResourceId(R.styleable.MessageCardView_message_icon, 0).apply {
                binding.icon.setImageResource(this)
            }

            attributes.getColor(R.styleable.MessageCardView_message_background_tint, 0).apply {
                binding.card.setCardBackgroundColor(this)
            }

            attributes.getResourceId(R.styleable.MessageCardView_message_icon_color, 0).apply {
                binding.icon.setColor(this)
            }

            attributes.getColor(R.styleable.MessageCardView_message_stroke_color, 0).apply {
                if (this != 0) {
                    binding.card.strokeWidth = 2
                    binding.card.strokeColor = this
                }
            }
        } finally {
            attributes.recycle()
        }
    }

    fun title(@StringRes resId: Int): MessageCardView {
        title(resources.getString(resId))
        return this
    }

    fun title(subtitle: String?): MessageCardView {
        binding.title.apply {
            text = subtitle
            visibility = if (subtitle != null) View.VISIBLE else View.GONE
        }
        return this
    }

    fun message(@StringRes resId: Int): MessageCardView {
        message(resources.getString(resId))
        return this
    }

    fun message(subtitle: String?): MessageCardView {
        binding.message.apply {
            text = subtitle
            visibility = if (subtitle != null) View.VISIBLE else View.GONE
        }
        return this
    }

    fun htmlMessage(
        html: String,
        linkClickedListener: (() -> Unit)? = null
    ): MessageCardView {
        binding.message.apply {
            setText(
                HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY),
                TextView.BufferType.SPANNABLE
            )
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

    fun action(
        label: String,
        endIcon: Drawable? = null,
        listener: OnClickListener
    ): MessageCardView {
        with(binding.action) {
            text = label
            setOnClickListener(listener)
            visibility = VISIBLE
            if (endIcon != null) {
                icon = endIcon
                iconGravity = ICON_GRAVITY_END
            }
        }
        return this
    }

    fun closeButton(listener: OnClickListener): MessageCardView {
        binding.closeButton.visibility = VISIBLE
        binding.closeButton.setOnClickListener(listener)
        return this
    }
}
