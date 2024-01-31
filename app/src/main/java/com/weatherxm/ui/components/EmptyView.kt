package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.airbnb.lottie.LottieDrawable.INFINITE
import com.weatherxm.R
import com.weatherxm.R.styleable.EmptyView_empty_action
import com.weatherxm.databinding.ViewEmptyBinding
import com.weatherxm.ui.common.setHtml
import me.saket.bettermovementmethod.BetterLinkMovementMethod

class EmptyView : LinearLayout {

    private lateinit var binding: ViewEmptyBinding

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
        binding = ViewEmptyBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL
        gravity = Gravity.CENTER

        this.context.theme.obtainStyledAttributes(attrs, R.styleable.EmptyView, 0, 0).apply {
            try {
                title(getString(R.styleable.EmptyView_empty_title))
                subtitle(getString(R.styleable.EmptyView_empty_subtitle))
                action(getString(EmptyView_empty_action))

                val prefilledAnimation = getResourceId(R.styleable.EmptyView_empty_animation, 0)
                if (prefilledAnimation != 0) {
                    animation(prefilledAnimation)
                }
            } finally {
                recycle()
            }
        }
    }

    fun title(@StringRes resId: Int): EmptyView {
        title(resources.getString(resId))
        return this
    }

    fun title(title: String?): EmptyView {
        binding.title.apply {
            text = title
            visibility = if (title != null) View.VISIBLE else View.GONE
        }
        return this
    }

    fun subtitle(@StringRes resId: Int): EmptyView {
        subtitle(resources.getString(resId))
        return this
    }

    fun subtitle(subtitle: String?): EmptyView {
        binding.subtitle.apply {
            text = subtitle
            visibility = if (!subtitle.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
        return this
    }

    fun htmlSubtitle(
        @StringRes resId: Int,
        arg: String? = null,
        linkClickedListener: (() -> Unit)? = null
    ): EmptyView {
        binding.subtitle.apply {
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

    fun htmlSubtitle(
        subtitle: String?,
        arg: String? = null,
        linkClickedListener: (() -> Unit)? = null
    ): EmptyView {
        if (subtitle == null) {
            visibility = GONE
            return this
        }
        binding.subtitle.apply {
            if (arg.isNullOrEmpty()) {
                setHtml(subtitle)
            } else {
                setHtml(subtitle, arg)
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

    fun animation(@RawRes res: Int, loop: Boolean = true): EmptyView {
        binding.animation.apply {
            setAnimation(res)
            repeatCount = if (loop) INFINITE else 0
            playAnimation()
        }
        return this
    }

    fun action(label: String?): EmptyView {
        binding.action.apply {
            text = label
            visibility = if (label != null) View.VISIBLE else View.GONE
        }
        return this
    }

    fun listener(listener: OnClickListener?): EmptyView {
        binding.action.setOnClickListener(listener)
        binding.action.visibility = if (listener != null) VISIBLE else GONE
        return this
    }

    fun clear(): EmptyView {
        title(null)
        subtitle(null)
        action(null)
        listener(null)
        return this
    }
}
