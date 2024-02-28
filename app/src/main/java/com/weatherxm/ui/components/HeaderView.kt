package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.weatherxm.R
import com.weatherxm.databinding.ViewHeaderBinding
import org.koin.core.component.KoinComponent

open class HeaderView : LinearLayout, KoinComponent {

    private lateinit var binding: ViewHeaderBinding

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
        binding = ViewHeaderBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL

        val attributes =
            this.context.theme.obtainStyledAttributes(attrs, R.styleable.HeaderView, 0, 0)

        attributes.getString(R.styleable.HeaderView_header_title)?.let {
            title(it)
        }
        attributes.recycle()
    }

    fun title(title: String): HeaderView {
        binding.title.text = title
        return this
    }

    fun subtitle(subtitle: String): HeaderView {
        binding.subtitle.text = subtitle
        return this
    }
}
