package com.weatherxm.ui.components.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherxm.R

@Suppress("FunctionNaming")
@Composable
fun QuestOnboardingCard(
    stepsCompleted: Int,
    totalWXM: Int,
    locationStepWXM: Int,
    notificationsStepWXM: Int,
    sensorsStepWXM: Int,
    walletStepWXM: Int,
    followStepWXM: Int,
    onCompleteTasks: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.colorSurface)),
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large)),
        elevation = CardDefaults.cardElevation(dimensionResource(R.dimen.elevation_small))
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.padding_normal))) {
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
                    modifier = Modifier.padding(start = dimensionResource(R.dimen.padding_normal))
                ) {
                    Title(
                        text = stringResource(R.string.onboarding_quest),
                        fontSize = 20.sp
                    )
                    SmallText(
                        text = stringResource(R.string.onboarding_quest_subtitle),
                        colorRes = R.color.darkGrey,
                        paddingValues = PaddingValues(top = dimensionResource(R.dimen.padding_small))
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
                TextQuestWXMAllocated(totalWXM)
                LinearProgressIndicator(
                    progress = { stepsCompleted.toFloat() / 5 },
                    modifier = Modifier
                        .padding(
                            start = dimensionResource(R.dimen.padding_normal),
                            end = dimensionResource(R.dimen.padding_small)
                        )
                        .height(12.dp)
                        .weight(1F),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    color = colorResource(R.color.colorPrimary),
                    trackColor = colorResource(R.color.colorBackground)
                )
                SmallText(stepsCompleted.toString(), colorRes = R.color.colorPrimary)
                SmallText("/5", colorRes = R.color.darkGrey)
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
@Preview
fun QuestOnboardingCardPreview() {
    QuestOnboardingCard(
        stepsCompleted = 2,
        totalWXM = 80,
        locationStepWXM = 20,
        notificationsStepWXM = 20,
        sensorsStepWXM = 20,
        walletStepWXM = 10,
        followStepWXM = 10
    ) { }
}
