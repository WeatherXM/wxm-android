package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.unit.dp
import com.weatherxm.R
import com.weatherxm.data.models.Reward
import com.weatherxm.data.models.RewardsAnnotationGroup
import com.weatherxm.data.models.SeverityLevel
import com.weatherxm.databinding.ViewDailyRewardsCardBinding
import com.weatherxm.ui.common.ActionForMessageView
import com.weatherxm.ui.common.Contracts.EMPTY_VALUE
import com.weatherxm.ui.common.DataForMessageView
import com.weatherxm.ui.common.SubtitleForMessageView
import com.weatherxm.ui.common.setCardStroke
import com.weatherxm.ui.common.setColor
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.compose.MessageCardView
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.NumberUtils.formatTokens
import com.weatherxm.util.Rewards.getRewardIcon
import com.weatherxm.util.Rewards.getRewardScoreColor
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
        binding.dailyRewardTimestamp.visible(formattedTimestamp != null)

        binding.reward.text = context.getString(R.string.reward, formatTokens(data.totalReward))

        binding.baseRewardIcon.setImageDrawable(
            AppCompatResources.getDrawable(context, getRewardIcon(data.baseRewardScore))
        )

        binding.baseRewardIcon.setColorFilter(
            context.getColor(getRewardScoreColor(data.baseRewardScore))
        )
        binding.baseRewardScore.text =
            context.getString(R.string.wxm_amount, formatTokens(data.baseReward))

        if (data.totalBoostReward == null) {
            binding.boostsIcon.setImageDrawable(
                AppCompatResources.getDrawable(context, R.drawable.ic_error_hex_filled)
            )
            binding.boostsIcon.setColor(R.color.midGrey)
            binding.boosts.visible(false)
            binding.noActiveBoosts.visible(true)
        } else {
            binding.boostsIcon.setImageDrawable(
                AppCompatResources.getDrawable(context, R.drawable.ic_checkmark_hex_filled)
            )
            binding.boostsIcon.setColor(R.color.blue)
            binding.boosts.text = context.getString(
                R.string.wxm_amount, formatTokens(data.totalBoostReward)
            )
            binding.noActiveBoosts.visible(false)
            binding.boosts.visible(true)
        }
        binding.viewRewardDetails.visible(
            onViewDetails != null && data.annotationSummary.isNullOrEmpty()
        )
        setupAnnotations(data.annotationSummary, useShortAnnotationText, onViewDetails)
    }

    private fun setupAnnotations(
        annotations: List<RewardsAnnotationGroup>?,
        useShortAnnotationText: Boolean,
        onViewDetails: (() -> Unit)? = null
    ) {
        val sortedSeverities = annotations?.map { it.severityLevel }?.sortedByDescending { it }

        if (sortedSeverities.isNullOrEmpty()) {
            binding.annotationCard.visible(false)
            binding.parentCard.strokeWidth = 0
            return
        }

        if (sortedSeverities.contains(SeverityLevel.ERROR)) {
            onAnnotation(
                SeverityLevel.ERROR,
                getAnnotationMessage(useShortAnnotationText, sortedSeverities, annotations.size),
                onViewDetails
            )
        } else if (sortedSeverities.contains(SeverityLevel.WARNING)) {
            onAnnotation(
                SeverityLevel.WARNING,
                getAnnotationMessage(useShortAnnotationText, sortedSeverities, annotations.size),
                onViewDetails
            )
        } else if (sortedSeverities.contains(SeverityLevel.INFO)) {
            onAnnotation(
                SeverityLevel.INFO,
                getAnnotationMessage(useShortAnnotationText, sortedSeverities, annotations.size),
                onViewDetails
            )
        }
    }

    private fun getAnnotationMessage(
        useShortAnnotationText: Boolean,
        sortedSeverities: List<SeverityLevel?>,
        annotationsSize: Int
    ): String {
        return if (!useShortAnnotationText) {
            if (sortedSeverities.contains(SeverityLevel.ERROR)) {
                context.getString(R.string.annotation_error_text)
            } else if (sortedSeverities.contains(SeverityLevel.WARNING)) {
                context.getString(R.string.annotation_warn_text)
            } else {
                context.getString(R.string.annotation_info_text)
            }
        } else {
            if (sortedSeverities[0] == SeverityLevel.INFO) {
                resources.getQuantityString(
                    R.plurals.annotation_issue_info_text, annotationsSize, annotationsSize
                )
            } else {
                resources.getQuantityString(
                    R.plurals.annotation_issue_warn_error_text, annotationsSize, annotationsSize
                )
            }
        }
    }

    private fun onAnnotation(
        severityLevel: SeverityLevel,
        text: String,
        onViewDetails: (() -> Unit)? = null
    ) {
        val strokeColor: Int = when (severityLevel) {
            SeverityLevel.ERROR -> R.color.error
            SeverityLevel.WARNING -> R.color.warning
            SeverityLevel.INFO -> R.color.infoStrokeColor
        }

        binding.parentCard.setCardStroke(strokeColor, 2)

        binding.annotationCard.setContent {
            MessageCardView(
                data = DataForMessageView(
                    extraTopPadding = 24.dp,
                    subtitle = SubtitleForMessageView(htmlMessageAsString = text),
                    action = onViewDetails?.let {
                        ActionForMessageView(label = R.string.view_reward_details) {
                            onViewDetails.invoke()
                        }
                    },
                    severityLevel = severityLevel
                )
            )
        }
        binding.annotationCard.visible(true)
    }
}
