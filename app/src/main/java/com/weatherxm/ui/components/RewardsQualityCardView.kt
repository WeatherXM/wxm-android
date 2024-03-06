package com.weatherxm.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.weatherxm.R
import com.weatherxm.databinding.ViewRewardQualityCardBinding
import com.weatherxm.ui.common.setCardStroke
import com.weatherxm.ui.common.setColor
import com.weatherxm.ui.common.setVisible
import com.weatherxm.util.Rewards.getRewardScoreColor
import org.koin.core.component.KoinComponent

open class RewardsQualityCardView : LinearLayout, KoinComponent {

    private lateinit var binding: ViewRewardQualityCardBinding

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
        binding = ViewRewardQualityCardBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL

        val attributes =
            this.context.theme.obtainStyledAttributes(
                attrs, R.styleable.RewardsQualityCardView, 0, 0
            )

        attributes.getString(R.styleable.RewardsQualityCardView_reward_quality_title)?.let {
            title(it)
        }
        attributes.recycle()
    }

    fun title(title: String): RewardsQualityCardView {
        binding.title.text = title
        return this
    }

    fun desc(description: String): RewardsQualityCardView {
        binding.statusDesc.text = description
        return this
    }

    fun warning(): RewardsQualityCardView {
        binding.parentCard.setCardStroke(R.color.warning, 2)
        binding.statusIcon.setImageResource(R.drawable.ic_warning_hex_filled)
        return this
    }

    fun error(): RewardsQualityCardView {
        binding.parentCard.setCardStroke(R.color.error, 2)
        binding.statusIcon.setImageResource(R.drawable.ic_error_hex_filled)
        return this
    }

    fun checkmark(): RewardsQualityCardView {
        binding.statusIcon.setImageResource(R.drawable.ic_checkmark_hex_filled)
        return this
    }

    fun setIconColor(score: Int): RewardsQualityCardView {
        binding.statusIcon.setColor(getRewardScoreColor(score))
        return this
    }

    fun setSlider(score: Int): RewardsQualityCardView {
        binding.slider.values = listOf(score.toFloat())
        binding.slider.trackActiveTintList =
            ColorStateList.valueOf(context.getColor(getRewardScoreColor(score)))
        binding.sliderContainer.setVisible(true)
        return this
    }

    fun infoButton(listener: () -> Unit): RewardsQualityCardView {
        binding.infoButton.setOnClickListener {
            listener.invoke()
        }
        binding.infoButton.setVisible(true)
        return this
    }
}