package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.weatherxm.databinding.ViewInfoBannerCardBinding
import com.weatherxm.ui.common.visible

class InfoBannerCardView : LinearLayout {

    private lateinit var binding: ViewInfoBannerCardBinding

    constructor(context: Context?) : super(context) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context?) {
        binding = ViewInfoBannerCardBinding.inflate(LayoutInflater.from(context), this)
    }

    fun title(title: String): InfoBannerCardView {
        binding.title.text = title
        binding.title.visible(title.isNotEmpty())
        return this
    }

    fun message(message: String): InfoBannerCardView {
        binding.message.text = message
        binding.message.visible(message.isNotEmpty())
        return this
    }

    fun action(label: String, showButton: Boolean, onClick: () -> Unit): InfoBannerCardView {
        binding.actionBtn.text = label
        binding.actionBtn.setOnClickListener {
            onClick.invoke()
        }
        binding.actionBtn.visible(showButton)
        return this
    }

    fun close(showCloseButton: Boolean, onClick: () -> Unit): InfoBannerCardView {
        binding.closeBtn.setOnClickListener {
            onClick.invoke()
        }
        binding.closeBtn.visible(showCloseButton)
        return this
    }
}
