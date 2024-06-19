package com.weatherxm.ui.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.button.MaterialButton.ICON_GRAVITY_END
import com.google.android.material.button.MaterialButton.ICON_GRAVITY_START
import com.weatherxm.R
import com.weatherxm.databinding.ViewMessageCardBinding
import com.weatherxm.ui.common.hide
import com.weatherxm.ui.common.setCardStroke
import com.weatherxm.ui.common.setColor
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.visible
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
            } ?: binding.title.visible(false)

            val text = attributes.getString(R.styleable.MessageCardView_message_message)
            val htmlText = attributes.getString(R.styleable.MessageCardView_message_html_message)
            if (text != null) {
                message(text)
            } else if (htmlText != null) {
                htmlMessage(htmlText)
            } else {
                binding.message.hide(null)
            }

            binding.closeButton.visible(
                attributes.getBoolean(
                    R.styleable.MessageCardView_message_includes_close_button, true
                )
            )

            attributes.getResourceId(R.styleable.MessageCardView_message_icon, 0).apply {
                if (this != 0) {
                    binding.icon.setImageResource(this)
                }
                binding.icon.visible(this != 0)
            }

            attributes.getColor(R.styleable.MessageCardView_message_background_tint, 0).apply {
                binding.card.setCardBackgroundColor(this)
            }

            attributes.getResourceId(R.styleable.MessageCardView_message_icon_color, 0).apply {
                if (this != 0) {
                    binding.icon.setColor(this)
                }
            }

            attributes.getDimension(R.styleable.MessageCardView_message_card_radius, 0F).apply {
                if (this != 0F) {
                    binding.card.radius = this
                }
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

    fun title(title: String?): MessageCardView {
        binding.title.apply {
            text = title
            visible(title != null)
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
            visible(subtitle != null)
        }
        return this
    }

    fun setBackground(@ColorRes colorResId: Int): MessageCardView {
        binding.card.setCardBackgroundColor(context.getColor(colorResId))
        return this
    }

    fun warning(includeStroke: Boolean = false): MessageCardView {
        binding.icon.setImageDrawable(
            AppCompatResources.getDrawable(context, R.drawable.ic_warning_hex_filled)
        )
        binding.icon.visible(true)
        binding.card.setCardBackgroundColor(context.getColor(R.color.warningTint))
        if (includeStroke) {
            binding.card.setCardStroke(R.color.warning, 2)
        }
        return this
    }

    fun error(includeStroke: Boolean = false): MessageCardView {
        binding.icon.setImageDrawable(
            AppCompatResources.getDrawable(context, R.drawable.ic_error_hex_filled)
        )
        binding.icon.visible(true)
        binding.card.setCardBackgroundColor(context.getColor(R.color.errorTint))
        if (includeStroke) {
            binding.card.setCardStroke(R.color.error, 2)
        }
        return this
    }

    fun htmlMessage(
        html: String,
        linkClickedListener: (() -> Unit)? = null
    ): MessageCardView {
        binding.message.apply {
            setHtml(html)
            if (linkClickedListener != null) {
                movementMethod = BetterLinkMovementMethod.newInstance().apply {
                    setOnLinkClickListener { _, _ ->
                        linkClickedListener.invoke()
                        return@setOnLinkClickListener true
                    }
                }
            }
            visible(true)
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
            visible(true)
            if (endIcon != null) {
                icon = endIcon
                iconGravity = ICON_GRAVITY_END
            }
        }
        return this
    }

    fun actionPrimaryBtn(
        label: String,
        startIcon: Drawable? = null,
        listener: OnClickListener
    ): MessageCardView {
        with(binding.actionPrimary) {
            text = label
            setOnClickListener(listener)
            visible(true)
            if (startIcon != null) {
                icon = startIcon
                iconGravity = ICON_GRAVITY_START
            }
        }
        return this
    }

    fun closeButton(listener: OnClickListener): MessageCardView {
        binding.closeButton.visible(true)
        binding.closeButton.setOnClickListener(listener)
        return this
    }
}
