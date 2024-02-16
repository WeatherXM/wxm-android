package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.weatherxm.R
import com.weatherxm.databinding.ViewDailyRewardsCardBinding
import com.weatherxm.ui.common.UIRewardObject
import com.weatherxm.ui.common.setVisible
import com.weatherxm.util.Rewards.formatTokens
import com.weatherxm.util.Rewards.getRewardScoreColor
import com.weatherxm.util.Weather.EMPTY_VALUE
import org.koin.core.component.KoinComponent

open class DailyRewardsCardView : LinearLayout, KoinComponent {

    private lateinit var binding: ViewDailyRewardsCardBinding

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
        binding = ViewDailyRewardsCardBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL
        gravity = Gravity.CENTER
    }

    @Suppress("MagicNumber")
    fun updateUI(data: UIRewardObject?, onViewDetails: (() -> Unit)? = null) {
        if (data == null) return

        if (onViewDetails != null) {
            binding.viewRewardDetails.setOnClickListener {
                onViewDetails.invoke()
            }
        } else {
            binding.viewRewardDetails.setVisible(false)
        }

        binding.dailyRewardTimestamp.text =
            context.getString(R.string.earnings_for, data.referenceDate ?: EMPTY_VALUE)
        binding.dailyRewardTimestamp.setVisible(data.rewardFormattedTimestamp != null)

        binding.reward.text =
            context.getString(R.string.reward, formatTokens(data.actualReward.toBigDecimal()))

        binding.baseRewardIcon.setColorFilter(
            context.getColor(getRewardScoreColor(data.rewardScore))
        )

        binding.baseRewardScore.text =
            context.getString(R.string.wxm_amount, formatTokens(data.baseReward.toBigDecimal()))

        binding.boosts.setVisible(data.boosts != null)
        binding.noActiveBoosts.setVisible(data.boosts == null)
        data.boosts?.let {
            binding.boosts.text =
                context.getString(R.string.wxm_amount, formatTokens(it.toBigDecimal()))
        }
    }
}
