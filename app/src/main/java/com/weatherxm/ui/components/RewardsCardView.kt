package com.weatherxm.ui.components

import android.content.Context
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.databinding.ViewRewardsCardBinding
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIRewardObject
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.devicedetails.rewards.RewardsViewModel
import com.weatherxm.util.Analytics
import com.weatherxm.util.DateTimeHelper.getFormattedOffset
import com.weatherxm.util.DateTimeHelper.isUTC
import com.weatherxm.util.Rewards.formatLostRewards
import com.weatherxm.util.Rewards.formatTokens
import com.weatherxm.util.Rewards.getRewardAnnotationBackgroundColor
import com.weatherxm.util.Rewards.getRewardScoreColor
import com.weatherxm.util.setRewardStatusChip
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

open class RewardsCardView : LinearLayout, KoinComponent {

    private lateinit var binding: ViewRewardsCardBinding
    private val analytics: Analytics by inject()

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
        binding = ViewRewardsCardBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL
        gravity = Gravity.CENTER
    }

    @Suppress("MagicNumber")
    fun updateUI(
        data: UIRewardObject?,
        device: UIDevice?,
        tabSelected: RewardsViewModel.TabSelected? = null,
        onInfoButton: (String, String) -> Unit,
        onProblems: (() -> Unit)? = null
    ) {
        if (data == null) return

        binding.timestamp.text = data.rewardFormattedTimestamp
        binding.timestamp.setVisible(data.rewardFormattedTimestamp != null)

        val actualReward = data.actualReward ?: 0F
        binding.reward.text =
            resources.getString(R.string.reward, formatTokens(actualReward.toBigDecimal()))

        binding.scoreExplanation.setOnClickListener {
            trackLearnMore(Analytics.ParamValue.REWARDS_SCORE.paramValue)
            onInfoButton.invoke(
                context.getString(R.string.reward_score),
                context.getString(
                    R.string.reward_score_desc,
                    context.getString(R.string.docs_url_reward_mechanism)
                )
            )
        }

        binding.maxRewardsExplanation.setOnClickListener {
            trackLearnMore(Analytics.ParamValue.MAX_REWARDS.paramValue)
            onInfoButton.invoke(
                context.getString(R.string.max_rewards),
                context.getString(
                    R.string.max_rewards_desc,
                    context.getString(R.string.docs_url_reward_mechanism)
                )
            )
        }

        binding.timelineExplanation.setOnClickListener {
            trackLearnMore(Analytics.ParamValue.TIMELINE.paramValue)
            val description = if (device?.lastWeatherStationActivity?.offset?.isUTC() == true) {
                context.getString(
                    R.string.timeline_desc_utc_only,
                    context.getString(R.string.docs_url_reward_mechanism)
                )
            } else {
                context.getString(
                    R.string.timeline_desc,
                    device?.lastWeatherStationActivity?.offset?.getFormattedOffset(),
                    context.getString(R.string.docs_url_reward_mechanism)
                )
            }
            onInfoButton.invoke(context.getString(R.string.timeline), description)
        }

        data.rewardScore?.let {
            binding.rewardStatus.setRewardStatusChip(it)
            binding.score.text = resources.getString(R.string.score, (it.toFloat() / 100))
            binding.scoreIcon.setColorFilter(
                resources.getColor(getRewardScoreColor(data.rewardScore), context.theme)
            )
            binding.rewardStatus.setVisible(true)
        } ?: kotlin.run {
            binding.score.text = resources.getString(R.string.score_unknown)
            binding.rewardStatus.setVisible(false)
        }

        data.periodMaxReward?.let {
            binding.maxRewards.text = formatTokens(it.toBigDecimal())
        }

        binding.secondaryInfoContainer.setVisible(
            data.rewardScore != 0 && data.periodMaxReward != 0F
        )
        if (tabSelected != null &&
            (data.annotations.isNotEmpty() || ((data.rewardScore) ?: 0) < 100)
        ) {
            setErrorData(data, device?.relation, onProblems)
        } else {
            binding.problemsCard.setVisible(false)
        }

        setTimelineData(data, tabSelected)
    }

    private fun setErrorData(
        data: UIRewardObject,
        deviceRelation: DeviceRelation?,
        onProblems: (() -> Unit)? = null
    ) {
        val actionMessage = context.getString(
            if (deviceRelation == DeviceRelation.OWNED) {
                R.string.identify_fix_problems
            } else {
                R.string.see_detailed_problems
            }
        )
        with(binding.problemsCard) {
            val backgroundColorResId = getRewardAnnotationBackgroundColor(data.rewardScore)
            setBackground(backgroundColorResId)
            action(actionMessage) {
                onProblems?.invoke()
            }
            if (data.lostRewards == 0F && data.periodMaxReward == 0F) {
                htmlMessage(context.getString(R.string.problems_found_desc_no_rewards))
            } else if (((data.lostRewards) ?: 0F) == 0F) {
                message(R.string.problems_found_desc_without_lost_rewards)
            } else {
                val lostRewards = formatLostRewards(data.lostRewards)
                htmlMessage(context.getString(R.string.problems_found_desc, lostRewards))
            }
            setVisible(true)
        }
    }

    private fun setTimelineData(data: UIRewardObject, tabSelected: RewardsViewModel.TabSelected?) {
        binding.singleLineTimeline.setVisible(data.timelineScores.isNotEmpty())
        binding.timelineChart.setVisible(data.timelineScores.isNotEmpty())

        binding.timelineChart.setContent {
            BarChart(data, tabSelected)
        }

        binding.timelineTitle.text = data.timelineTitle
        binding.singleLineTimeline.setVisible(data.timelineTitle != null)
    }

    @Suppress("FunctionNaming")
    @Composable
    internal fun BarChart(data: UIRewardObject, tabSelected: RewardsViewModel.TabSelected?) {
        val barSpaceDimenId = when (tabSelected) {
            RewardsViewModel.TabSelected.LATEST -> R.dimen.padding_extra_small
            RewardsViewModel.TabSelected.LAST_WEEK -> R.dimen.padding_normal
            RewardsViewModel.TabSelected.LAST_MONTH -> R.dimen.padding_extra_extra_small
            null -> R.dimen.padding_extra_small
        }

        Column(
            modifier = Modifier.then(
                Modifier.fillMaxWidth()
            )
        ) {
            Row(
                modifier = Modifier.then(
                    Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                data.timelineScores.forEach { item ->
                    Bar(
                        color = Color(context.getColor(getRewardScoreColor(item))),
                        dimensionResource(id = barSpaceDimenId)
                    )
                }
            }
            Row(
                modifier = Modifier.then(
                    Modifier.fillMaxWidth()
                ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                TimelineTexts(data, tabSelected)
            }
        }
    }

    @Suppress("FunctionNaming")
    @Composable
    private fun TimelineTexts(data: UIRewardObject, tabSelected: RewardsViewModel.TabSelected?) {
        if (tabSelected == RewardsViewModel.TabSelected.LATEST || tabSelected == null) {
            val startHour: String
            val midHour: String
            val endHour: String
            if (DateFormat.is24HourFormat(context)) {
                startHour = "00:00"
                midHour = "12:00"
                endHour = "23:00"
            } else {
                startHour = context.getString(R.string.start_of_day_hour)
                midHour = context.getString(R.string.mid_of_day_hour)
                endHour = context.getString(R.string.end_of_day_hour)
            }
            Text(
                text = startHour,
                fontSize = 12.sp,
                color = Color(context.getColor(R.color.colorOnSurface)),
            )
            Text(
                text = midHour,
                fontSize = 12.sp,
                color = Color(context.getColor(R.color.colorOnSurface)),
            )
            Text(
                text = endHour,
                fontSize = 12.sp,
                color = Color(context.getColor(R.color.colorOnSurface)),
            )
        } else {
            Text(
                text = data.fromDate ?: "",
                fontSize = 12.sp,
                color = Color(context.getColor(R.color.colorOnSurface)),
            )
            Text(
                text = data.toDate ?: "",
                fontSize = 12.sp,
                color = Color(context.getColor(R.color.colorOnSurface)),
            )
        }
    }

    @Suppress("FunctionNaming")
    @Composable
    private fun RowScope.Bar(color: Color, barSpace: Dp) {
        val radius = dimensionResource(id = R.dimen.card_corner_radius_small)
        Spacer(
            modifier = Modifier
                .padding(horizontal = barSpace)
                .height(70.dp)
                .weight(1f)
                .background(color, RoundedCornerShape(radius, radius, radius, radius))
        )
    }

    private fun trackLearnMore(itemClicked: String) {
        analytics.trackEventSelectContent(
            Analytics.ParamValue.LEARN_MORE.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_ID, itemClicked)
        )
    }
}
