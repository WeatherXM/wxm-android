package com.weatherxm.ui.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import com.google.android.material.button.MaterialButton.ICON_GRAVITY_END
import com.weatherxm.R
import com.weatherxm.databinding.ViewErrorCardBinding
import com.weatherxm.util.setHtml
import me.saket.bettermovementmethod.BetterLinkMovementMethod

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

        binding.action.visibility = GONE

        this.context.theme.obtainStyledAttributes(attrs, R.styleable.ErrorCardView, 0, 0).apply {
            try {
                title(getString(R.styleable.ErrorCardView_error_title))
                message(getString(R.styleable.ErrorCardView_error_message))
                if (!getBoolean(R.styleable.ErrorCardView_error_includes_close_button, true)) {
                    binding.closeButton.visibility = View.GONE
                }
                if (!getBoolean(R.styleable.ErrorCardView_error_includes_stroke, true)) {
                    binding.card.strokeWidth = 0
                }
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

    fun htmlMessage(
        @StringRes resId: Int,
        arg: String? = null,
        linkClickedListener: (() -> Unit)? = null
    ): ErrorCardView {
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

    fun action(label: String, endIcon: Drawable? = null, listener: OnClickListener): ErrorCardView {
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

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }
}
