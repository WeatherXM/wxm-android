package com.weatherxm.ui.managesubscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
        elevation = CardDefaults.cardElevation(
            defaultElevation = dimensionResource(R.dimen.elevation_normal)
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, colorResource(R.color.colorPrimary))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.margin_normal_to_large)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_normal))
        ) {
            // Checkmark icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = colorResource(R.color.crypto_opacity_15),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_checkmark_only),
                    contentDescription = null,
                    tint = colorResource(R.color.darkGrey),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_small_to_normal))
            ) {
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
                        Card(
                            shape = RoundedCornerShape(dimensionResource(R.dimen.radius_extra_extra_large)),
                            colors = CardDefaults.cardColors(
                                containerColor = colorResource(R.color.cryptoInverse)
                            )
                        ) {
                            SmallText(
                                text = stringResource(R.string.current_plan).uppercase(),
                                colorRes = R.color.colorOnSurface,
                                fontWeight = FontWeight.Bold,
                                paddingValues = PaddingValues(
                                    horizontal = dimensionResource(R.dimen.margin_small_to_normal),
                                    vertical = dimensionResource(R.dimen.margin_extra_small)
                                )
                            )
                        }
                    }
                }

                LargeText(
                    text = "$0",
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )

                Column(
                    verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_small))
                ) {
                    FeatureItem(text = stringResource(R.string.free_plan_first_benefit))
                    FeatureItem(text = stringResource(R.string.free_plan_second_benefit))
                    FeatureItem(text = stringResource(R.string.free_plan_third_benefit))
                    FeatureItem(text = stringResource(R.string.free_plan_fourth_benefit))
                }
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun FeatureItem(text: String) {
    Row(
        horizontalArrangement = spacedBy(dimensionResource(R.dimen.margin_small)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_checkmark_only),
            contentDescription = null,
            tint = colorResource(R.color.crypto),
            modifier = Modifier.size(14.dp)
        )
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
