package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.weatherxm.R
import com.weatherxm.data.SeverityLevel
import com.weatherxm.databinding.ViewDailyRewardsCardBinding
import com.weatherxm.ui.common.DailyReward
import com.weatherxm.ui.common.setCardStroke
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
    fun updateUI(data: DailyReward?, onViewDetails: (() -> Unit)? = null) {
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

        val severityLevels = data.annotationSummary.map {
            it.severityLevel
        }
        if (severityLevels.contains(SeverityLevel.ERROR)) {
            onAnnotation(
                R.color.errorTint, R.color.error, R.string.annotation_error_text, onViewDetails
            )
        } else if (severityLevels.contains(SeverityLevel.WARNING)) {
            onAnnotation(
                R.color.warningTint, R.color.warning, R.string.annotation_warn_text, onViewDetails
            )
        } else if (severityLevels.contains(SeverityLevel.INFO)) {
            onAnnotation(
                R.color.blueTint, R.color.colorPrimary, R.string.annotation_info_text, onViewDetails
            )
        } else {
            binding.annotationCard.setVisible(false)
            binding.viewRewardDetails.setVisible(true)
        }
    }

    private fun onAnnotation(
        @ColorRes backgroundColor: Int,
        @ColorRes strokeColor: Int,
        @StringRes text: Int,
        onViewDetails: (() -> Unit)? = null
    ) {
        binding.parentCard.setCardStroke(strokeColor, 2)
        binding.annotationCard.setBackgroundColor(context.getColor(backgroundColor))
        binding.annotationCard
            .setBackground(backgroundColor)
            .htmlMessage(context.getString(text))
            .action(context.getString(R.string.view_reward_details)) {
                onViewDetails?.invoke()
            }
            .setVisible(true)
        binding.viewRewardDetails.setVisible(false)
    }
}
