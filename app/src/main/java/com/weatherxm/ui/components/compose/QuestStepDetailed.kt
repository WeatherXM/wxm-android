package com.weatherxm.ui.components.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherxm.R
import com.weatherxm.ui.common.QuestStep
import com.weatherxm.ui.common.QuestStepType

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun QuestStepDetailed(data: QuestStep, onClick: () -> Unit) {
    val tokensCardColor = if (data.isSkipped) {
        R.color.errorTint
    } else if (data.isCompleted) {
        R.color.successTint
    } else {
        R.color.layer1
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = dimensionResource(R.dimen.padding_normal_to_large)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1F), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(24.dp),
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
            Column(
                modifier = Modifier.padding(
                    start = dimensionResource(R.dimen.padding_small_to_normal)
                )
            ) {
                LargeText(text = data.title, fontSize = 20.sp)
                MediumText(
                    text = data.description,
                    colorRes = R.color.darkGrey,
                    paddingValues = PaddingValues(
                        top = dimensionResource(R.dimen.padding_extra_small)
                    )
                )
            }
        }
        Card(
            modifier = Modifier.padding(
                start = dimensionResource(R.dimen.padding_small_to_normal)
            ),
            colors = CardDefaults.cardColors(containerColor = colorResource(tokensCardColor)),
            shape = RoundedCornerShape(dimensionResource(R.dimen.radius_small)),
        ) {
            Row(
                modifier = Modifier.padding(dimensionResource(R.dimen.padding_small)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (data.isSkipped) {
                    Text(
                        text = data.tokens.toString(),
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.colorOnSurface),
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = TextDecoration.LineThrough
                    )
                    Text(
                        text = "\$WXM",
                        color = colorResource(R.color.darkGrey),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        textDecoration = TextDecoration.LineThrough,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else {
                    TextQuestWXMAllocated(data.tokens, 16.sp)
                }
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
@Preview
fun QuestStepDetailedPreview() {
    QuestStepDetailed(
        QuestStep(
            id = "step1",
            title = "Enable Location",
            description = "Enable location to get hyper-local forecasts.",
            tokens = 10,
            isOptional = false,
            isCompleted = true,
            isSkipped = false,
            type = QuestStepType.ENABLE_LOCATION_PERMISSION
        )
    ) {}
}
