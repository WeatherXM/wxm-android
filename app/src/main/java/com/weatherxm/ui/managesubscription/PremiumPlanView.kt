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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
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
    hasFreeTrialAvailable: Boolean,
    onSelected: () -> Unit
) {
    if (sub == null) {
        return
    }

    val primaryColor = colorResource(R.color.colorPrimary)
    val successColor = colorResource(R.color.success)

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
            BorderStroke(2.dp, primaryColor)
        } else {
            BorderStroke(1.dp, colorResource(R.color.crypto_opacity_15))
        }
    ) {
        // Gradient overlay on top of card surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.12f),
                            Color.Transparent,
                            successColor.copy(alpha = 0.08f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.margin_normal_to_large)),
                verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_small_to_normal))
            ) {
                // Header row: title + badge
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
                        Box(
                            modifier = Modifier
                                .background(
                                    color = primaryColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = primaryColor.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .padding(
                                    horizontal = dimensionResource(R.dimen.margin_small_to_normal),
                                    vertical = dimensionResource(R.dimen.margin_extra_small)
                                )
                        ) {
                            SmallText(
                                text = stringResource(R.string.current_plan).uppercase(),
                                colorRes = R.color.colorPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .background(
                                    color = primaryColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = primaryColor.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .padding(
                                    horizontal = dimensionResource(R.dimen.margin_small_to_normal),
                                    vertical = dimensionResource(R.dimen.margin_extra_small)
                                ),
                            horizontalArrangement = spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_star_filled),
                                contentDescription = null,
                                tint = colorResource(R.color.colorPrimary),
                                modifier = Modifier.size(8.dp)
                            )
                            Text(
                                text = stringResource(R.string.best_accuracy),
                                color = colorResource(R.color.colorPrimary),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                lineHeight = 12.sp,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(
                                        includeFontPadding = false
                                    )
                                )
                            )
                        }
                    }
                }

                // Pricing section
                Column(verticalArrangement = spacedBy(4.dp)) {
                    if (hasFreeTrialAvailable) {
                        LargeText(
                            text = stringResource(R.string.two_months_free),
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        )
                        val thenNote = stringResource(
                            if (sub.id == PLAN_YEARLY) R.string.then_per_year
                            else R.string.then_per_month,
                            sub.price
                        )
                        MediumText(
                            text = listOf(
                                stringResource(R.string.two_months_free),
                                thenNote
                            ).joinToString(separator = ", "),
                            colorRes = R.color.darkGrey
                        )
                        MediumText(
                            text = stringResource(R.string.limited_launch_offer),
                            colorRes = R.color.darkGrey
                        )
                    } else {
                        Row(
                            horizontalArrangement = spacedBy(dimensionResource(
                                R.dimen.margin_small)
                            )
                        ) {
                            Text(
                                text = sub.price,
                                color = colorResource(R.color.colorOnSurface),
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                modifier = Modifier.alignByBaseline()
                            )
                            Text(
                                text = when (sub.id) {
                                    PLAN_MONTHLY -> stringResource(R.string.per_month)
                                    PLAN_YEARLY -> stringResource(R.string.per_year)
                                    else -> "/${sub.id}"
                                },
                                color = colorResource(R.color.darkGrey),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.alignByBaseline()
                            )
                        }
                        MediumText(
                            text = stringResource(R.string.limited_launch_price),
                            colorRes = R.color.darkGrey
                        )
                    }
                }

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colorResource(R.color.crypto_opacity_15))
                )

                // Description
                MediumText(
                    text = stringResource(R.string.premium_plan_description),
                    colorRes = R.color.darkGrey
                )

                // Features
                Column(verticalArrangement = spacedBy(9.dp)) {
                    val firstBenefit = AnnotatedString.Builder().apply {
                        append(stringResource(R.string.premium_plan_first_benefit))
                        append(" ")
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(stringResource(R.string.free))
                        pop()
                    }.toAnnotatedString()
                    PremiumFeatureItem(text = firstBenefit)
                    PremiumFeatureItem(
                        text = AnnotatedString(stringResource(R.string.premium_plan_second_benefit))
                    )
                    PremiumFeatureItem(
                        text = AnnotatedString(stringResource(R.string.premium_plan_third_benefit))
                    )
                    PremiumFeatureItem(
                        text = AnnotatedString(stringResource(R.string.premium_plan_fourth_benefit))
                    )
                }

            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun PremiumFeatureItem(text: AnnotatedString) {
    Row(
        horizontalArrangement = spacedBy(dimensionResource(R.dimen.margin_small_to_normal)),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(
                    color = colorResource(R.color.successTint),
                    shape = RoundedCornerShape(6.dp)
                )
                .border(
                    width = 1.dp,
                    color = colorResource(R.color.success).copy(alpha = 0.35f),
                    shape = RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_checkmark_only),
                contentDescription = null,
                tint = colorResource(R.color.textColor),
                modifier = Modifier.size(10.dp)
            )
        }
        Text(
            text = text,
            color = colorResource(R.color.colorOnSurface),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Suppress("UnusedPrivateMember", "FunctionNaming")
@PreviewLightDark
@Composable
private fun PreviewPremiumPlanView() {
    Column {
        PremiumPlanView(
            sub = SubscriptionOffer("monthly", "$0.99", "offerToken", null),
            isSelected = false,
            isCurrentPlan = false,
            hasFreeTrialAvailable = false
        ) {}
        PremiumPlanView(
            sub = SubscriptionOffer("monthly", "$0.99", "offerToken", null),
            isSelected = true,
            isCurrentPlan = false,
            hasFreeTrialAvailable = true
        ) {}
        PremiumPlanView(
            sub = SubscriptionOffer("monthly", "$0.99", "offerToken", null),
            isSelected = true,
            isCurrentPlan = true,
            hasFreeTrialAvailable = false
        ) {}
    }
}
