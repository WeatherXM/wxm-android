package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import com.weatherxm.R
import com.weatherxm.databinding.ViewHeliumSetFrequencyBinding
import com.weatherxm.ui.common.FrequencyState
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.visible

class HeliumSetFrequencyView : LinearLayout {

    private lateinit var binding: ViewHeliumSetFrequencyBinding

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
        binding = ViewHeliumSetFrequencyBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL
        gravity = Gravity.CENTER

        this.context.theme.obtainStyledAttributes(attrs, R.styleable.HeliumSetFrequencyView, 0, 0)
            .apply {
                try {
                    getBoolean(R.styleable.HeliumSetFrequencyView_is_on_claiming, true).apply {
                        binding.setButton.visible(this)
                        binding.twoButtonsContainer.visible(!this)
                    }
                } finally {
                    recycle()
                }
            }

        binding.confirmFrequencyToggle.setOnCheckedChangeListener { _, checked ->
            binding.setButton.isEnabled = checked
            binding.changeButton.isEnabled = checked
        }
    }

    fun defaultState(frequencyState: FrequencyState, isOnClaiming: Boolean) {
        if (frequencyState.country.isNullOrEmpty()) {
            binding.frequencySelectedText.visible(false)
        } else {
            binding.frequencySelectedText.text = if (isOnClaiming) {
                context.getString(
                    R.string.frequency_selected_text, frequencyState.country
                )
            } else {
                context.getString(
                    R.string.changing_frequency_selected_text, frequencyState.country
                )
            }
        }

        binding.frequenciesSelector.adapter = ArrayAdapter(
            context, android.R.layout.simple_spinner_dropdown_item, frequencyState.frequencies
        )
    }

    fun listener(
        onFrequencyDocumentation: (String) -> Unit,
        onBack: () -> Unit,
        onSet: (Int) -> Unit
    ) {
        with(binding.description) {
            movementMethod =
                me.saket.bettermovementmethod.BetterLinkMovementMethod.newInstance().apply {
                    setOnLinkClickListener { _, url ->
                        onFrequencyDocumentation(url)
                        return@setOnLinkClickListener true
                    }
                }
            setHtml(
                R.string.set_frequency_desc,
                this.context.getString(R.string.helium_frequencies_mapping_url)
            )
        }

        binding.backButton.setOnClickListener {
            onBack()
        }

        binding.setButton.setOnClickListener {
            onSet(binding.frequenciesSelector.selectedItemPosition)
        }

        binding.changeButton.setOnClickListener {
            onSet(binding.frequenciesSelector.selectedItemPosition)
        }
    }
}
