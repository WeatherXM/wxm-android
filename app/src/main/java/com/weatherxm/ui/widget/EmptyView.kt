package com.weatherxm.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.weatherxm.R
import com.weatherxm.databinding.ViewEmptyBinding

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
                action(getString(R.styleable.EmptyView_empty_action))
                animation(getResourceId(R.styleable.EmptyView_empty_animation, 0))
            } finally {
                recycle()
            }
        }
    }

    fun title(@StringRes res: Int): EmptyView {
        binding.title.text = resources.getString(res)
        return this
    }

    fun title(title: String?): EmptyView {
        binding.title.text = title
        return this
    }

    fun subtitle(subtitle: String?): EmptyView {
        binding.subtitle.text = subtitle
        return this
    }

    private fun animation(@RawRes res: Int): EmptyView {
        binding.animation.setAnimation(res)
        return this
    }

    fun action(label: String?): EmptyView {
        binding.action.text = label
        return this
    }

    fun listener(listener: OnClickListener?): EmptyView {
        binding.action.setOnClickListener(listener)
        binding.action.visibility = if (listener != null) VISIBLE else GONE
        return this
    }

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }
}
