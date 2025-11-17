package com.weatherxm.ui.managesubscription

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.Purchase
import com.weatherxm.R
import com.weatherxm.ui.components.compose.LargeText
import com.weatherxm.ui.components.compose.MediumText

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun CurrentPlanView(currentPurchase: Purchase?, onManageSubscription: () -> Unit) {
    Column(
        verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_normal))
    ) {
        LargeText(
            text = stringResource(R.string.current_plan),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.colorSurface)
            ),
            shape = RoundedCornerShape(dimensionResource(R.dimen.radius_small)),
            elevation = CardDefaults.cardElevation(
                dimensionResource(R.dimen.elevation_small)
            )
        ) {
            Column(
                modifier = Modifier.padding(dimensionResource(R.dimen.padding_normal)),
                verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_large))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LargeText(
                        text = if (currentPurchase == null) {
                            stringResource(R.string.standard)
                        } else {
                            stringResource(R.string.premium_forecast)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentPurchase?.isAutoRenewing == false) {
                                colorResource(R.color.error)
                            } else {
                                colorResource(R.color.colorPrimary)
                            }
                        )
                    ) {
                        Text(
                            text = if (currentPurchase?.isAutoRenewing == false) {
                                stringResource(R.string.canceled)
                            } else {
                                stringResource(R.string.active)
                            },
                            color = if (currentPurchase?.isAutoRenewing == false) {
                                colorResource(R.color.colorOnSurface)
                            } else {
                                colorResource(R.color.colorOnPrimary)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(
                                horizontal = dimensionResource(R.dimen.padding_small_to_normal),
                                vertical = dimensionResource(R.dimen.padding_extra_small)
                            )
                        )
                    }
                }

                if (currentPurchase == null) {
                    MediumText(
                        text = stringResource(R.string.just_the_basics),
                        colorRes = R.color.darkGrey
                    )
                } else if (!currentPurchase.isAutoRenewing) {
                    MediumText(
                        text = stringResource(R.string.canceled_subtitle),
                        colorRes = R.color.darkGrey
                    )
                } else {
                    Column(
                        verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_normal))
                    ) {
                        Row(
                            horizontalArrangement = spacedBy(
                                dimensionResource(R.dimen.margin_small_to_normal)
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_checkmark_only),
                                tint = colorResource(R.color.colorOnSurface),
                                modifier = Modifier
                                    .padding(top = dimensionResource(R.dimen.padding_extra_small))
                                    .size(16.dp),
                                contentDescription = null
                            )
                            Column(
                                verticalArrangement = spacedBy(
                                    dimensionResource(R.dimen.margin_extra_small)
                                )
                            ) {
                                LargeText(
                                    text = stringResource(R.string.mosaic_forecast),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                MediumText(
                                    text = stringResource(R.string.mosaic_forecast_explanation),
                                    colorRes = R.color.darkGrey
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = spacedBy(
                                dimensionResource(R.dimen.margin_small_to_normal)
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_checkmark_only),
                                tint = colorResource(R.color.colorOnSurface),
                                modifier = Modifier
                                    .padding(top = dimensionResource(R.dimen.padding_extra_small))
                                    .size(16.dp),
                                contentDescription = null
                            )
                            Column(
                                verticalArrangement = spacedBy(
                                    dimensionResource(R.dimen.margin_extra_small)
                                )
                            ) {
                                LargeText(
                                    text = stringResource(R.string.hourly_forecast),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                MediumText(
                                    text = stringResource(R.string.just_the_basics),
                                    colorRes = R.color.darkGrey
                                )
                            }
                        }
                    }
                }
                if(currentPurchase != null) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onManageSubscription,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.layer1),
                            contentColor = colorResource(R.color.colorPrimary)
                        ),
                        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium)),
                    ) {
                        MediumText(
                            stringResource(R.string.get_premium),
                            fontWeight = FontWeight.Bold,
                            colorRes = R.color.colorBackground
                        )
                    }
                }
            }
        }
    }
}
