package com.weatherxm.ui.components.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.weatherxm.R
import com.weatherxm.data.models.Reward
import com.weatherxm.data.models.RewardsAnnotationGroup
import com.weatherxm.data.models.SeverityLevel
import com.weatherxm.ui.common.ActionForMessageView
import com.weatherxm.ui.common.DataForMessageView
import com.weatherxm.ui.common.SubtitleForMessageView
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.NumberUtils.formatTokens
import com.weatherxm.util.Rewards.getRewardIcon
import com.weatherxm.util.Rewards.getRewardScoreColor
import java.time.ZonedDateTime

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun DailyRewardsView(
    data: Reward,
    useShortAnnotationText: Boolean,
    onCardClick: (() -> Unit)? = null,
    onViewDetails: (() -> Unit)? = null
) {
    val sortedSeverities = data.annotationSummary?.map {
        it.severityLevel
    }?.sortedByDescending { it }?.filterNotNull()
    val topSeverity = sortedSeverities?.getOrNull(0)

    val border = topSeverity?.let {
        when (it) {
            SeverityLevel.ERROR -> BorderStroke(1.dp, colorResource(R.color.error))
            SeverityLevel.WARNING -> BorderStroke(1.dp, colorResource(R.color.warning))
            SeverityLevel.INFO -> BorderStroke(1.dp, colorResource(R.color.infoStrokeColor))
        }
    }

    if (onCardClick != null) {
        CardViewClickable(
            borderStroke = border,
            elevationResource = R.dimen.elevation_small,
            onClickListener = { onCardClick() }
        ) {
            DailyRewardsContents(data, useShortAnnotationText, sortedSeverities, onViewDetails)
        }
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = colorResource(R.color.colorSurface)),
            border = border,
            elevation = CardDefaults.cardElevation(dimensionResource(R.dimen.elevation_small)),
            shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large))
        ) {
            DailyRewardsContents(data, useShortAnnotationText, sortedSeverities, onViewDetails)
        }
    }

}

@Suppress("LongMethod", "MagicNumber", "FunctionNaming")
@Composable
private fun DailyRewardsContents(
    data: Reward,
    useShortAnnotationText: Boolean,
    sortedSeverities: List<SeverityLevel>?,
    onViewDetails: (() -> Unit)? = null
) {
    Column(verticalArrangement = spacedBy((-16).dp)) {
        Card(
            modifier = Modifier.zIndex(1F),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.colorSurface)
            )
        ) {
            Column(
                modifier = Modifier.padding(dimensionResource(R.dimen.padding_normal_to_large))
            ) {
                LargeText(stringResource(R.string.daily_reward), fontWeight = FontWeight.Bold)

                data.timestamp?.getFormattedDate(true)?.let {
                    MediumText(
                        text = stringResource(R.string.earnings_for, it),
                        colorRes = R.color.colorOnSurfaceVariant
                    )
                }

                Text(
                    text = if (LocalInspectionMode.current) {
                        "+3.55 \$WXM"
                    } else {
                        stringResource(R.string.reward, formatTokens(data.totalReward))
                    },
                    color = colorResource(R.color.colorPrimaryVariant),
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.padding_large))
                )

                HorizontalDivider(
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.padding_small)),
                    color = colorResource(R.color.layer2)
                )

                Row(
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.padding_small)),
                    horizontalArrangement = spacedBy(dimensionResource(R.dimen.padding_large))
                ) {
                    RewardWithIconAndLabel(
                        reward = data.baseReward,
                        iconResId = getRewardIcon(data.baseRewardScore),
                        iconColor = getRewardScoreColor(data.baseRewardScore),
                        label = stringResource(R.string.base_reward),
                    )
                    if (data.totalBoostReward != null) {
                        RewardWithIconAndLabel(
                            reward = data.totalBoostReward,
                            iconResId = R.drawable.ic_checkmark_hex_filled,
                            iconColor = R.color.blue,
                            label = stringResource(R.string.boosts),
                            fallbackOnNullReward = stringResource(R.string.no_active)
                        )
                    } else {
                        RewardWithIconAndLabel(
                            reward = null,
                            iconResId = R.drawable.ic_error_hex_filled,
                            iconColor = R.color.midGrey,
                            label = stringResource(R.string.boosts),
                            fallbackOnNullReward = stringResource(R.string.no_active)
                        )
                    }
                }

                if (onViewDetails != null && data.annotationSummary.isNullOrEmpty()) {
                    Button(
                        onClick = { onViewDetails() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.layer1),
                            contentColor = colorResource(R.color.colorPrimary)
                        ),
                        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = dimensionResource(R.dimen.padding_large))
                    ) {
                        MediumText(
                            text = stringResource(R.string.view_reward_details),
                            fontWeight = FontWeight.Bold,
                            colorRes = R.color.colorPrimary
                        )
                    }
                }
            }
        }

        if (!sortedSeverities.isNullOrEmpty() && data.annotationSummary != null) {
            val annotationMessage = getAnnotationMessage(
                useShortAnnotationText,
                sortedSeverities,
                data.annotationSummary.size
            )

            MessageCardView(
                data = DataForMessageView(
                    extraTopPadding = 16.dp,
                    subtitle = SubtitleForMessageView(htmlMessageAsString = annotationMessage),
                    action = onViewDetails?.let {
                        ActionForMessageView(label = R.string.view_reward_details) {
                            onViewDetails.invoke()
                        }
                    },
                    severityLevel = sortedSeverities[0]
                )
            )
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun RewardWithIconAndLabel(
    reward: Float?,
    iconResId: Int,
    iconColor: Int,
    label: String,
    fallbackOnNullReward: String? = null
) {
    Row {
        Icon(
            painter = painterResource(iconResId),
            tint = colorResource(iconColor),
            modifier = Modifier.size(20.dp),
            contentDescription = null
        )
        Column(modifier = Modifier.padding(start = dimensionResource(R.dimen.padding_small))) {
            if (reward != null || fallbackOnNullReward == null) {
                SmallText(
                    text = if (LocalInspectionMode.current) {
                        "3.55 \$WXM"
                    } else {
                        stringResource(R.string.wxm_amount, formatTokens(reward))
                    },
                    fontWeight = FontWeight.Bold,
                    colorRes = R.color.colorOnSurfaceVariant
                )
            } else {
                SmallText(text = fallbackOnNullReward, colorRes = R.color.colorOnSurfaceVariant)
            }
            SmallText(text = label, colorRes = R.color.colorOnSurfaceVariant)
        }
    }
}

@Composable
private fun getAnnotationMessage(
    useShortAnnotationText: Boolean,
    sortedSeverities: List<SeverityLevel?>,
    annotationsSize: Int
): String {
    return if (!useShortAnnotationText) {
        if (sortedSeverities.contains(SeverityLevel.ERROR)) {
            stringResource(R.string.annotation_error_text)
        } else if (sortedSeverities.contains(SeverityLevel.WARNING)) {
            stringResource(R.string.annotation_warn_text)
        } else {
            stringResource(R.string.annotation_info_text)
        }
    } else {
        if (sortedSeverities[0] == SeverityLevel.INFO) {
            pluralStringResource(
                R.plurals.annotation_issue_info_text,
                annotationsSize,
                annotationsSize
            )
        } else {
            pluralStringResource(
                R.plurals.annotation_issue_warn_error_text,
                annotationsSize,
                annotationsSize
            )
        }
    }
}

@Suppress("FunctionNaming", "MagicNumber")
@Preview
@Composable
fun PreviewDailyRewardsView() {
    DailyRewardsView(
        data = Reward(
            ZonedDateTime.now(),
            3.55F,
            3.55F,
            3.55F,
            100,
            mutableListOf(
                RewardsAnnotationGroup(
                    SeverityLevel.ERROR,
                    "group",
                    "Title of error",
                    "Message of error",
                    null
                )
            )
        ),
        false
    ) {}
}
