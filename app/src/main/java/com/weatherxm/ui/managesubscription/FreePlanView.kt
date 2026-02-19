package com.weatherxm.ui.managesubscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherxm.R
import com.weatherxm.ui.components.compose.LargeText
import com.weatherxm.ui.components.compose.MediumText
import com.weatherxm.ui.components.compose.SmallText

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun FreePlanView(isSelected: Boolean, isCurrentPlan: Boolean, onSelected: () -> Unit) {
    Card(
        onClick = onSelected,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.margin_large),
                end = dimensionResource(R.dimen.margin_large),
                top = dimensionResource(R.dimen.margin_normal)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.colorSurface)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isSelected && isCurrentPlan) {
            BorderStroke(2.dp, colorResource(R.color.colorPrimary))
        } else if (isSelected) {
            BorderStroke(2.dp, colorResource(R.color.warning))
        } else {
            BorderStroke(1.dp, colorResource(R.color.crypto_opacity_15))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.margin_normal_to_large)),
            verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_small_to_normal))
        ) {
            // Header row: title + current plan badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                LargeText(
                    text = stringResource(R.string.free),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                if (isCurrentPlan) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = colorResource(R.color.crypto_opacity_15),
                                shape = RoundedCornerShape(999.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = colorResource(R.color.darkGrey).copy(alpha = 0.25f),
                                shape = RoundedCornerShape(999.dp)
                            )
                            .padding(
                                horizontal = dimensionResource(R.dimen.margin_small_to_normal),
                                vertical = dimensionResource(R.dimen.margin_extra_small)
                            )
                    ) {
                        SmallText(
                            text = stringResource(R.string.current_plan),
                            colorRes = R.color.darkGrey,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Price + tagline
            Column(verticalArrangement = spacedBy(4.dp)) {
                LargeText(
                    text = "$0",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                MediumText(
                    text = stringResource(R.string.free_plan_tagline),
                    colorRes = R.color.darkGrey
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colorResource(R.color.crypto_opacity_15))
            )

            // Features
            Column(verticalArrangement = spacedBy(9.dp)) {
                FreeFeatureItem(stringResource(R.string.free_plan_first_benefit))
                FreeFeatureItem(stringResource(R.string.free_plan_second_benefit))
                FreeFeatureItem(stringResource(R.string.free_plan_third_benefit))
                FreeFeatureItem(stringResource(R.string.free_plan_fourth_benefit))
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun FreeFeatureItem(text: String) {
    Row(
        horizontalArrangement = spacedBy(dimensionResource(R.dimen.margin_small_to_normal)),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(
                    color = colorResource(R.color.crypto_opacity_15),
                    shape = RoundedCornerShape(6.dp)
                )
                .border(
                    width = 1.dp,
                    color = colorResource(R.color.darkGrey).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "•",
                color = colorResource(R.color.darkGrey),
                fontSize = 14.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center
            )
        }
        MediumText(text = text, colorRes = R.color.darkestBlue)
    }
}

@Suppress("UnusedPrivateMember", "FunctionNaming")
@Preview
@Composable
private fun PreviewFreePlanView() {
    Column {
        FreePlanView(isSelected = false, isCurrentPlan = false) {}
        FreePlanView(isSelected = true, isCurrentPlan = false) {}
        FreePlanView(isSelected = true, isCurrentPlan = true) {}
    }
}
