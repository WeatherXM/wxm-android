package com.weatherxm.ui.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.TextView.BufferType.SPANNABLE
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import com.google.android.material.button.MaterialButton.ICON_GRAVITY_END
import com.weatherxm.R
import com.weatherxm.databinding.ViewWarningCardBinding
import com.weatherxm.ui.common.hide
import com.weatherxm.util.setHtml
import me.saket.bettermovementmethod.BetterLinkMovementMethod

class WarningCardView : FrameLayout {

    private lateinit var binding: ViewWarningCardBinding

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

    private fun init(context: Context, attrs: AttributeSet? = null) {
        binding = ViewWarningCardBinding.inflate(LayoutInflater.from(context), this)

        this.context.theme.obtainStyledAttributes(attrs, R.styleable.WarningCardView, 0, 0).apply {
            try {
                getString(R.styleable.WarningCardView_warning_title)?.let {
                    title(it)
                } ?: binding.title.hide(null)

                getString(R.styleable.WarningCardView_warning_message)?.let {
                    message(it)
                } ?: binding.message.hide(null)

                if (!getBoolean(R.styleable.WarningCardView_warning_includes_close_button, true)) {
                    binding.closeButton.visibility = View.GONE
                }
            } finally {
                recycle()
            }
        }
    }

    fun title(@StringRes resId: Int): WarningCardView {
        title(resources.getString(resId))
        return this
    }

    fun title(subtitle: String?): WarningCardView {
        binding.title.apply {
            text = subtitle
            visibility = if (subtitle != null) View.VISIBLE else View.GONE
        }
        return this
    }

    fun message(@StringRes resId: Int): WarningCardView {
        message(resources.getString(resId))
        return this
    }

    fun message(message: String?): WarningCardView {
        binding.message.apply {
            text = message
            visibility = if (message != null) View.VISIBLE else View.GONE
        }
        return this
    }
    fun htmlMessage(
        html: String,
        linkClickedListener: (() -> Unit)? = null
    ): WarningCardView {
        binding.message.apply {
            setText(HtmlCompat.fromHtml(html, FROM_HTML_MODE_LEGACY), SPANNABLE)
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

    fun htmlMessage(
        @StringRes resId: Int,
        arg: String? = null,
        linkClickedListener: (() -> Unit)? = null
    ): WarningCardView {
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

    fun closeButton(listener: OnClickListener): WarningCardView {
        binding.closeButton.visibility = VISIBLE
        binding.closeButton.setOnClickListener(listener)
        return this
    }

    fun action(
        label: String,
        endIcon: Drawable? = null,
        actionWithBorders: Boolean = false,
        listener: OnClickListener
    ): WarningCardView {
        if (actionWithBorders) {
            with(binding.actionOutline) {
                text = label
                setOnClickListener(listener)
                if (endIcon != null) {
                    icon = endIcon
                    iconGravity = ICON_GRAVITY_END
                }
                binding.action.visibility = GONE
                visibility = VISIBLE
            }
        } else {
            with(binding.action) {
                text = label
                setOnClickListener(listener)
                if (endIcon != null) {
                    icon = endIcon
                    iconGravity = ICON_GRAVITY_END
                }
                binding.actionOutline.visibility = GONE
                visibility = VISIBLE
            }
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
