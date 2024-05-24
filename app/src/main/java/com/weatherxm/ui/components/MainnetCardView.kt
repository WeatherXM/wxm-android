package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.weatherxm.databinding.ViewMainnetCardBinding

class MainnetCardView : LinearLayout {

    private lateinit var binding: ViewMainnetCardBinding

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
        binding = ViewMainnetCardBinding.inflate(LayoutInflater.from(context), this)
    }

    fun message(message: String): MainnetCardView {
        binding.mainnetMessage.text = message
        return this
    }

    fun listener(onClick: () -> Unit): MainnetCardView {
        binding.mainnetCard.setOnClickListener {
            onClick.invoke()
        }
        return this
    }
}
