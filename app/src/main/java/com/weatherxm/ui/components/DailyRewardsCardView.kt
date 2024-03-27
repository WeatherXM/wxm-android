package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import com.weatherxm.R
import com.weatherxm.data.Reward
import com.weatherxm.data.RewardsAnnotationGroup
import com.weatherxm.data.SeverityLevel
import com.weatherxm.databinding.ViewDailyRewardsCardBinding
import com.weatherxm.ui.common.setCardStroke
import com.weatherxm.ui.common.setColor
import com.weatherxm.ui.common.setVisible
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.Rewards.formatTokens
import com.weatherxm.util.Rewards.getRewardIcon
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

    fun updateUI(
        data: Reward?,
        useShortAnnotationText: Boolean,
        onViewDetails: (() -> Unit)? = null
    ) {
        if (data == null) return

        if (onViewDetails != null) {
            binding.viewRewardDetails.setOnClickListener {
                onViewDetails.invoke()
            }
        }

        val formattedTimestamp = data.timestamp?.getFormattedDate(true)
        binding.dailyRewardTimestamp.text =
            context.getString(R.string.earnings_for, formattedTimestamp ?: EMPTY_VALUE)
        binding.dailyRewardTimestamp.setVisible(formattedTimestamp != null)

        binding.reward.text = data.totalReward?.let {
            context.getString(R.string.reward, formatTokens(it.toBigDecimal()))
        } ?: EMPTY_VALUE

        binding.baseRewardIcon.setImageDrawable(
            AppCompatResources.getDrawable(context, getRewardIcon(data.baseRewardScore))
        )

        binding.baseRewardIcon.setColorFilter(
            context.getColor(getRewardScoreColor(data.baseRewardScore))
        )

        binding.baseRewardScore.text = data.baseReward?.let {
            context.getString(R.string.wxm_amount, formatTokens(it.toBigDecimal()))
        } ?: EMPTY_VALUE

        if (data.totalBoostReward == null) {
            binding.boostsIcon.setImageDrawable(
                AppCompatResources.getDrawable(context, R.drawable.ic_error_hex_filled)
            )
            binding.boostsIcon.setColor(R.color.midGrey)
            binding.boosts.setVisible(false)
            binding.noActiveBoosts.setVisible(true)
        } else {
            binding.boostsIcon.setImageDrawable(
                AppCompatResources.getDrawable(context, R.drawable.ic_checkmark_hex_filled)
            )
            binding.boostsIcon.setColor(R.color.blue)
            binding.boosts.text = context.getString(
                R.string.wxm_amount, formatTokens(data.totalBoostReward.toBigDecimal())
            )
            binding.noActiveBoosts.setVisible(false)
            binding.boosts.setVisible(true)
        }
        binding.viewRewardDetails.setVisible(
            onViewDetails != null && data.annotationSummary.isNullOrEmpty()
        )
        setupAnnotations(data.annotationSummary, useShortAnnotationText, onViewDetails)
    }

    private fun setupAnnotations(
        annotations: List<RewardsAnnotationGroup>?,
        useShortAnnotationText: Boolean,
        onViewDetails: (() -> Unit)? = null
    ) {
        val severityLevels = annotations?.map {
            it.severityLevel
        }
        val annotationsSize = annotations?.size ?: 0

        if (severityLevels.isNullOrEmpty()) {
            binding.annotationCard.setVisible(false)
            binding.parentCard.strokeWidth = 0
            return
        }

        if (severityLevels.contains(SeverityLevel.ERROR)) {
            onAnnotation(
                SeverityLevel.ERROR,
                getAnnotationMessage(useShortAnnotationText, severityLevels, annotationsSize),
                onViewDetails
            )
        } else if (severityLevels.contains(SeverityLevel.WARNING)) {
            onAnnotation(
                SeverityLevel.WARNING,
                getAnnotationMessage(useShortAnnotationText, severityLevels, annotationsSize),
                onViewDetails
            )
        } else if (severityLevels.contains(SeverityLevel.INFO)) {
            onAnnotation(
                SeverityLevel.INFO,
                getAnnotationMessage(useShortAnnotationText, severityLevels, annotationsSize),
                onViewDetails
            )
        }
    }

    private fun getAnnotationMessage(
        useShortAnnotationText: Boolean,
        severityLevels: List<SeverityLevel?>,
        annotationsSize: Int
    ): String {
        return if (!useShortAnnotationText) {
            if (severityLevels.contains(SeverityLevel.ERROR)) {
                context.getString(R.string.annotation_error_text)
            } else if (severityLevels.contains(SeverityLevel.WARNING)) {
                context.getString(R.string.annotation_warn_text)
            } else {
                context.getString(R.string.annotation_info_text)
            }
        } else {
            if (severityLevels.contains(SeverityLevel.INFO) && annotationsSize > 1) {
                context.getString(R.string.annotation_issues_info_text, annotationsSize)
            } else if (severityLevels.contains(SeverityLevel.INFO) && annotationsSize <= 1) {
                context.getString(R.string.annotation_issue_info_text)
            } else if (annotationsSize > 1) {
                context.getString(R.string.annotation_issues_warn_error_text, annotationsSize)
            } else {
                context.getString(R.string.annotation_issue_warn_error_text)
            }
        }
    }

    private fun onAnnotation(
        severityLevel: SeverityLevel,
        text: String,
        onViewDetails: (() -> Unit)? = null
    ) {
        val strokeColor: Int
        val backgroundColor: Int
        when (severityLevel) {
            SeverityLevel.ERROR -> {
                strokeColor = R.color.error
                backgroundColor = R.color.errorTint
            }
            SeverityLevel.WARNING -> {
                strokeColor = R.color.warning
                backgroundColor = R.color.warningTint
            }
            SeverityLevel.INFO -> {
                strokeColor = R.color.infoStrokeColor
                backgroundColor = R.color.blueTint
            }
        }

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
    }
}
