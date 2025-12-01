package com.weatherxm.ui.managesubscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherxm.R
import com.weatherxm.data.models.SubscriptionOffer
import com.weatherxm.service.OFFER_FREE_TRIAL
import com.weatherxm.service.PLAN_MONTHLY
import com.weatherxm.service.PLAN_YEARLY
import com.weatherxm.ui.components.compose.LargeText
import com.weatherxm.ui.components.compose.MediumText
import com.weatherxm.ui.components.compose.SmallText

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun PlansView(plans: List<SubscriptionOffer>, onContinue: (SubscriptionOffer?) -> Unit) {
    var selectedPlan by remember { mutableStateOf(plans.firstOrNull()) }

    Column(verticalArrangement = Arrangement.SpaceBetween) {
        Column(
            modifier = Modifier.weight(1F),
            verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_normal))
        ) {
            LargeText(
                text = stringResource(R.string.select_a_plan),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            LazyColumn {
                items(plans) {
                    Plan(it, it == selectedPlan) {
                        selectedPlan = it
                    }
                }
            }
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onContinue(selectedPlan) },
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.colorPrimary),
                contentColor = colorResource(R.color.colorBackground)
            ),
            shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium)),
        ) {
            MediumText(
                stringResource(R.string.action_continue),
                fontWeight = FontWeight.Bold,
                colorRes = R.color.colorBackground
            )
        }
    }
}

@Suppress("FunctionNaming", "LongMethod")
@Composable
private fun Plan(
    sub: SubscriptionOffer,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(R.dimen.padding_large)),
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_small)),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                colorResource(R.color.colorSurface)
            } else {
                colorResource(R.color.layer1)
            }
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
                .padding(
                    top = dimensionResource(R.dimen.margin_normal),
                    end = dimensionResource(R.dimen.margin_normal),
                    bottom = dimensionResource(R.dimen.margin_normal)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = colorResource(R.color.colorPrimary),
                    unselectedColor = colorResource(R.color.colorPrimary)
                )
            )
            Column(
                modifier = Modifier.weight(1F),
                verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_small))
            ) {
                SmallText(
                    text = when (sub.id) {
                        PLAN_MONTHLY -> stringResource(R.string.monthly)
                        PLAN_YEARLY -> stringResource(R.string.annually)
                        else -> sub.id
                    },
                    colorRes = R.color.darkGrey
                )
                LargeText(
                    text = when (sub.id) {
                        PLAN_MONTHLY -> "${sub.price}${stringResource(R.string.per_month)}"
                        PLAN_YEARLY -> "${sub.price}${stringResource(R.string.per_year)}"
                        else -> "${sub.price}/${sub.id}"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                if (sub.offerId == OFFER_FREE_TRIAL) {
                    Card(
                        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_extra_small)),
                        colors = CardDefaults.cardColors(
                            containerColor = colorResource(R.color.successTint)
                        )
                    ) {
                        SmallText(
                            text = stringResource(R.string.one_month_free_trial),
                            colorRes = R.color.success,
                            paddingValues = PaddingValues(
                                horizontal = dimensionResource(R.dimen.margin_small_to_normal),
                                vertical = 6.dp
                            )
                        )
                    }
                }
                val subtitle = if (sub.offerId != null) {
                    buildString {
                        if (sub.id == PLAN_MONTHLY) {
                            append(stringResource(R.string.then_per_month, sub.price))
                            append(" ")
                        } else if (sub.id == PLAN_YEARLY) {
                            append(stringResource(R.string.then_per_year, sub.price))
                            append(" ")
                        }
                        append(stringResource(R.string.cancel_anytime))
                    }
                } else {
                    stringResource(R.string.cancel_anytime)
                }
                MediumText(
                    text = subtitle,
                    colorRes = R.color.darkGrey
                )
            }
        }
    }
}

@Suppress("UnusedPrivateMember", "FunctionNaming")
@Preview
@Composable
private fun PreviewPlans() {
    PlansView(
        plans = listOf(
            SubscriptionOffer("monthly", "3.99$", "offerToken", "free-trial"),
            SubscriptionOffer("yearly", "39.99$", "offerToken", null),
        )
    ) { }
}
