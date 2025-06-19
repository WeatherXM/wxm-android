package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import com.github.mikephil.charting.data.Entry
import com.weatherxm.R
import com.weatherxm.databinding.ViewNetworkStatsCardBinding
import com.weatherxm.ui.common.removeLinksUnderline
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.visible
import com.weatherxm.util.initializeNetworkStatsChart
import me.saket.bettermovementmethod.BetterLinkMovementMethod

class NetworkStatsCardView : LinearLayout {

    private lateinit var binding: ViewNetworkStatsCardBinding

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
        binding = ViewNetworkStatsCardBinding.inflate(LayoutInflater.from(context), this)

        this.context.theme.obtainStyledAttributes(attrs, R.styleable.NetworkStatsCardView, 0, 0)
            .apply {
                try {
                    binding.title.text = getString(R.styleable.NetworkStatsCardView_title)
                    binding.mainTitle.text =
                        getString(R.styleable.NetworkStatsCardView_main_value_title)
                    binding.firstSubCardTitle.text =
                        getString(R.styleable.NetworkStatsCardView_first_sub_card_title)
                    binding.secondSubCardTitle.text =
                        getString(R.styleable.NetworkStatsCardView_second_sub_card_title)
                } finally {
                    recycle()
                }
            }
    }

    fun updateHeader(
        subtitleResId: Int? = null,
        subtitleArg: String? = null,
        onSubtitleClick: ((String) -> Unit)? = null,
        onInfo: (() -> Unit)? = null,
        onOpen: (() -> Unit)? = null
    ): NetworkStatsCardView {
        if (subtitleResId != null && subtitleArg != null && onSubtitleClick != null) {
            binding.subtitle.movementMethod =
                BetterLinkMovementMethod.newInstance().apply {
                    setOnLinkClickListener { _, url ->
                        onSubtitleClick(url)
                        return@setOnLinkClickListener true
                    }
                }
            binding.subtitle.setHtml(subtitleResId, subtitleArg)
            binding.subtitle.removeLinksUnderline()
            binding.subtitle.visible(true)
        }
        if (onInfo != null) {
            binding.dataInfoBtn.visible(true)
            binding.dataInfoBtn.setOnClickListener {
                onInfo()
            }
        }
        if (onOpen != null) {
            binding.arrowRight.visible(true)
            binding.dataCard.setOnClickListener {
                onOpen()
            }
        }
        return this
    }

    fun updateMainData(
        mainValue: String,
        chartEntries: List<Entry>,
        chartDateStart: String,
        chartDateEnd: String
    ): NetworkStatsCardView {
        binding.mainValue.text = mainValue
        binding.dataChart.initializeNetworkStatsChart(chartEntries)
        binding.dataStartMonth.text = chartDateStart
        binding.dataEndMonth.text = chartDateEnd
        return this
    }

    fun updateFirstSubCard(
        value: String,
        onIconClick: (() -> Unit)? = null
    ): NetworkStatsCardView {
        binding.firstSubCardValue.text = value
        if (onIconClick != null) {
            binding.firstSubCardInfoBtn.visible(true)
            binding.firstSubCardInfoBtn.setOnClickListener {
                onIconClick()
            }
        }
        return this
    }

    fun updateSecondSubCard(
        value: String,
        valueTextColor: Int? = null,
        iconResId: Int? = null,
        onCardClick: (() -> Unit)? = null,
        onIconClick: (() -> Unit)? = null
    ): NetworkStatsCardView {
        binding.secondSubCardValue.text = value
        valueTextColor?.let {
            binding.secondSubCardValue.setTextColor(
                context.getColor(valueTextColor)
            )
        }
        if (iconResId != null) {
            binding.secondSubCardBtn.setImageDrawable(
                ResourcesCompat.getDrawable(resources, iconResId, context.theme)
            )
            binding.secondSubCardBtn.visible(true)
        }
        onIconClick?.let { binding.secondSubCardBtn.setOnClickListener { it() } }
        onCardClick?.let { binding.secondSubCard.setOnClickListener { it() } }
        return this
    }
}
