package com.weatherxm.ui.components.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.weatherxm.R
import com.weatherxm.ui.common.QuestOnboardingData
import com.weatherxm.ui.common.QuestStep

@Suppress("FunctionNaming", "LongMethod", "MagicNumber")
@Composable
fun QuestOnboardingCard(data: QuestOnboardingData, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.layer1)),
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large)),
        elevation = CardDefaults.cardElevation(dimensionResource(R.dimen.elevation_small))
    ) {
        Column(verticalArrangement = spacedBy((-16).dp)) {
            Card(
                modifier = Modifier.zIndex(1F),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.colorSurface)
                ),
                onClick = onClick
            ) {
                Column(
                    modifier = Modifier.padding(
                        dimensionResource(R.dimen.padding_normal)
                    )
                ) {
                    Row {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_polygon),
                                contentDescription = null,
                                tint = colorResource(R.color.colorBackground)
                            )
                            Icon(
                                modifier = Modifier.size(37.dp),
                                painter = painterResource(R.drawable.ic_user_checked),
                                contentDescription = null,
                                tint = colorResource(R.color.colorPrimary)
                            )
                        }
                        Column(
                            modifier = Modifier.padding(
                                start = dimensionResource(R.dimen.padding_normal)
                            )
                        ) {
                            Title(
                                text = data.title,
                                fontSize = 20.sp
                            )
                            SmallText(
                                text = if (data.isCompleted) {
                                    stringResource(R.string.quest_completed)
                                } else {
                                    data.description
                                },
                                colorRes = R.color.darkGrey,
                                paddingValues = PaddingValues(
                                    top = dimensionResource(R.dimen.padding_small)
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .padding(top = dimensionResource(R.dimen.padding_small))
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextQuestWXMAllocated(data.totalWXM, 18.sp)
                        LinearProgressIndicator(
                            progress = { data.stepsDone.toFloat() / data.steps.size },
                            modifier = Modifier
                                .padding(
                                    start = dimensionResource(R.dimen.padding_normal),
                                    end = dimensionResource(R.dimen.padding_small)
                                )
                                .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_large)))
                                .height(12.dp)
                                .weight(1F),
                            strokeCap = StrokeCap.Butt,
                            gapSize = 0.dp,
                            color = colorResource(R.color.colorPrimary),
                            trackColor = colorResource(R.color.colorBackground)
                        )
                        SmallText(data.stepsDone.toString(), colorRes = R.color.colorPrimary)
                        SmallText("/5", colorRes = R.color.darkGrey)
                    }
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.padding_normal)),
                onClick = onClick,
                colors = CardDefaults.cardColors(containerColor = colorResource(R.color.layer1))
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = spacedBy(dimensionResource(R.dimen.padding_normal))
                ) {
                    if (!data.isCompleted) {
                        data.steps.forEach { QuestStepCompact(it) }
                        Button(
                            onClick = onClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.colorPrimary),
                                contentColor = colorResource(R.color.colorOnPrimary)
                            ),
                            shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MediumText(
                                text = if (data.areAllStepsDone()) {
                                    stringResource(R.string.all_set)
                                } else {
                                    stringResource(R.string.complete_tasks)
                                },
                                fontWeight = FontWeight.Bold,
                                colorRes = R.color.colorOnPrimary
                            )
                        }
                    } else {
                        MediumText(stringResource(R.string.your_rewards_are_on_the_way))
                    }
                }
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun QuestStepCompact(data: QuestStep) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(
                    if (data.isCompleted || data.isSkipped) {
                        R.drawable.ic_checkmark_only
                    } else {
                        R.drawable.ic_dot
                    }
                ),
                contentDescription = null,
                tint = colorResource(
                    if (data.isCompleted || data.isSkipped) {
                        R.color.colorPrimary
                    } else {
                        R.color.darkGrey
                    }
                )
            )
            MediumText(
                text = data.title,
                paddingValues = PaddingValues(
                    start = dimensionResource(R.dimen.padding_small_to_normal)
                )
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextQuestWXMAllocated(data.tokens, 14.sp)
        }
    }
}

@Suppress("FunctionNaming")
@Composable
@Preview
fun QuestOnboardingCardPreview() {
    QuestOnboardingCard(
        QuestOnboardingData(
            title = "Onboarding Quest",
            description = "Complete onboarding to earn rewards!",
            isCompleted = false,
            stepsDone = 2,
            totalWXM = 40,
            steps = listOf(
                QuestStep(
                    id = "step1",
                    title = "Enable Location",
                    description = "Enable location to get hyper-local forecasts.",
                    tokens = 10,
                    isOptional = false,
                    isCompleted = true,
                    isSkipped = false,
                    type = "enable_location_permission"
                ),
                QuestStep(
                    id = "step2",
                    title = "Enable Notifications",
                    description = "Turn on notifications for quest tips.",
                    tokens = 5,
                    isOptional = true,
                    isCompleted = false,
                    isSkipped = true,
                    type = "enable_notifications"
                ),
                QuestStep(
                    id = "step3",
                    title = "Enable Environment Sensors",
                    description = "Let your phone’s sensors help us improve weather validation.",
                    tokens = 5,
                    isOptional = false,
                    isCompleted = true,
                    isSkipped = false,
                    type = "enable_environment_sensors"
                ),
                QuestStep(
                    id = "step4",
                    title = "Connect Wallet",
                    description = "Connect your wallet to receive rewards in the next airdrop.",
                    tokens = 15,
                    isOptional = false,
                    isCompleted = false,
                    isSkipped = false,
                    type = "connect_wallet"
                ),
                QuestStep(
                    id = "step5",
                    title = "Follow us on X",
                    description = "Stay updated with our latest news and announcements.",
                    tokens = 5,
                    isOptional = true,
                    isCompleted = false,
                    isSkipped = false,
                    type = "social_follow_x"
                )
            )
        )
    ) { }
}
