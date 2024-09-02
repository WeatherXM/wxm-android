package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.weatherxm.databinding.ViewSurveyCardBinding
import com.weatherxm.ui.common.visible

class SurveyCardView : LinearLayout {

    private lateinit var binding: ViewSurveyCardBinding

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
        binding = ViewSurveyCardBinding.inflate(LayoutInflater.from(context), this)
    }

    fun title(title: String): SurveyCardView {
        binding.title.text = title
        binding.title.visible(title.isNotEmpty())
        return this
    }

    fun message(message: String): SurveyCardView {
        binding.message.text = message
        binding.message.visible(message.isNotEmpty())
        return this
    }

    fun action(label: String, onClick: () -> Unit): SurveyCardView {
        binding.actionBtn.text = label
        binding.actionBtn.setOnClickListener {
            onClick.invoke()
        }
        binding.actionBtn.visible(label.isNotEmpty())
        return this
    }

    fun close(onClick: () -> Unit): SurveyCardView {
        binding.closeBtn.setOnClickListener {
            onClick.invoke()
        }
        return this
    }
}
