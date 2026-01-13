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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherxm.R
import com.weatherxm.data.models.SubscriptionOffer
import com.weatherxm.service.PLAN_MONTHLY
import com.weatherxm.service.PLAN_YEARLY
import com.weatherxm.ui.components.compose.LargeText
import com.weatherxm.ui.components.compose.MediumText
import com.weatherxm.ui.components.compose.SmallText

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun PremiumPlanView(
    sub: SubscriptionOffer?,
    isSelected: Boolean,
    isCurrentPlan: Boolean,
    onSelected: () -> Unit
) {
    if (sub == null) {
        return
    }

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
            horizontalArrangement = spacedBy(dimensionResource(R.dimen.margin_normal))
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
                    painter = painterResource(R.drawable.ic_sparkles),
                    contentDescription = null,
                    tint = colorResource(R.color.textColor),
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
                        text = stringResource(R.string.premium),
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

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = spacedBy(dimensionResource(R.dimen.margin_small))
                ) {
                    LargeText(
                        text = sub.price,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    MediumText(
                        text = when (sub.id) {
                            PLAN_MONTHLY -> stringResource(R.string.per_month)
                            PLAN_YEARLY -> stringResource(R.string.per_year)
                            else -> "/${sub.id}"
                        },
                        colorRes = R.color.darkGrey
                    )
                }

                SmallText(
                    text = stringResource(R.string.premium_plan_description),
                    colorRes = R.color.darkestBlue
                )

                // Features list
                Column(
                    verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_small))
                ) {
                    val firstBenefit = AnnotatedString.Builder().apply {
                        append(stringResource(R.string.premium_plan_first_benefit))
                        append(" ")
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(stringResource(R.string.free))
                        pop()
                    }.toAnnotatedString()
                    FeatureItem(text = firstBenefit)
                    FeatureItem(text = AnnotatedString(stringResource(R.string.premium_plan_second_benefit)))
                    FeatureItem(text = AnnotatedString(stringResource(R.string.premium_plan_third_benefit)))
                    FeatureItem(text = AnnotatedString(stringResource(R.string.premium_plan_fourth_benefit)))
                }
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun FeatureItem(text: AnnotatedString) {
    Row(
        horizontalArrangement = spacedBy(dimensionResource(R.dimen.margin_small_to_normal)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_checkmark_only),
            contentDescription = null,
            tint = colorResource(R.color.colorPrimary),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            color = colorResource(R.color.colorOnSurface),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Suppress("UnusedPrivateMember", "FunctionNaming")
@Preview
@Composable
private fun PreviewPremiumPlanView() {
    Column {
        PremiumPlanView(
            sub = SubscriptionOffer("monthly", "$4.99", "offerToken", null),
            isSelected = false,
            isCurrentPlan = false
        ) {}
        PremiumPlanView(
            sub = SubscriptionOffer("yearly", "$39.99", "offerToken", null),
            isSelected = true,
            isCurrentPlan = false
        ) {}
        PremiumPlanView(
            sub = SubscriptionOffer("yearly", "$39.99", "offerToken", null),
            isSelected = true,
            isCurrentPlan = true
        ) {}
    }
}
