package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.weatherxm.R
import com.weatherxm.data.Reward
import com.weatherxm.data.RewardsAnnotationGroup
import com.weatherxm.data.SeverityLevel
import com.weatherxm.databinding.ViewDailyRewardsCardBinding
import com.weatherxm.ui.common.setCardStroke
import com.weatherxm.ui.common.setVisible
import com.weatherxm.util.DateTimeHelper.getFormattedDate
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
    fun updateUI(
        data: Reward?,
        isInRewardDetails: Boolean,
        onViewDetails: (() -> Unit)? = null
    ) {
        if (data == null) return

        if (onViewDetails != null && !isInRewardDetails) {
            binding.viewRewardDetails.setOnClickListener {
                onViewDetails.invoke()
            }
        } else {
            binding.viewRewardDetails.setVisible(false)
        }

        val formattedTimestamp = data.timestamp?.getFormattedDate(true)
        binding.dailyRewardTimestamp.text =
            context.getString(R.string.earnings_for, formattedTimestamp ?: EMPTY_VALUE)
        binding.dailyRewardTimestamp.setVisible(formattedTimestamp != null)

        binding.reward.text = data.totalReward?.let {
            context.getString(R.string.reward, formatTokens(it.toBigDecimal()))
        } ?: EMPTY_VALUE

        binding.baseRewardIcon.setColorFilter(
            context.getColor(getRewardScoreColor(data.baseRewardScore))
        )

        binding.baseRewardScore.text = data.baseReward?.let {
            context.getString(R.string.wxm_amount, formatTokens(it.toBigDecimal()))
        } ?: EMPTY_VALUE

        binding.boosts.setVisible(data.totalBoostReward != null)
        binding.noActiveBoosts.setVisible(data.totalBoostReward == null)
        data.totalBoostReward?.let {
            binding.boosts.text =
                context.getString(R.string.wxm_amount, formatTokens(it.toBigDecimal()))
        }

        // TODO: Temporary code until we use something else for the reward details screen
        if (isInRewardDetails) {
            binding.parentCard.elevation = 0F
            binding.annotationCard.setVisible(false)
            return
        }
        setupAnnotations(data.annotationSummary, onViewDetails)
    }

    private fun setupAnnotations(
        annotations: List<RewardsAnnotationGroup>?,
        onViewDetails: (() -> Unit)? = null
    ) {
        val severityLevels = annotations?.map {
            it.severityLevel
        } ?: mutableListOf()
        val annotationsSize = annotations?.size ?: 0

        val message = if(severityLevels.contains(SeverityLevel.INFO) && annotationsSize > 1) {
            context.getString(R.string.annotations_info_text, annotationsSize)
        } else if(severityLevels.contains(SeverityLevel.INFO) && annotationsSize <= 1) {
            context.getString(R.string.annotation_info_text)
        } else if(!severityLevels.contains(SeverityLevel.INFO) && annotationsSize > 1) {
            context.getString(R.string.annotations_warn_error_text, annotationsSize)
        } else {
            context.getString(R.string.annotation_warn_error_text)
        }

        if (severityLevels.contains(SeverityLevel.ERROR)) {
            onAnnotation(R.color.errorTint, R.color.error, message, onViewDetails)
        } else if (severityLevels.contains(SeverityLevel.WARNING)) {
            onAnnotation(R.color.warningTint, R.color.warning, message, onViewDetails)
        } else if (severityLevels.contains(SeverityLevel.INFO)) {
            onAnnotation(R.color.blueTint, R.color.colorPrimary, message, onViewDetails)
        } else {
            binding.annotationCard.setVisible(false)
            binding.viewRewardDetails.setVisible(true)
        }
    }

    private fun onAnnotation(
        @ColorRes backgroundColor: Int,
        @ColorRes strokeColor: Int,
        text: String,
        onViewDetails: (() -> Unit)? = null
    ) {
        binding.parentCard.setCardStroke(strokeColor, 2)

        with(binding.annotationCard) {
            setBackgroundColor(context.getColor(backgroundColor))
            setBackground(backgroundColor)
            htmlMessage(text)
            if (onViewDetails != null) {
                action(context.getString(R.string.view_reward_details)) {
                    onViewDetails.invoke()
                }
            }
            setVisible(true)
        }
        binding.viewRewardDetails.setVisible(false)
    }
}
