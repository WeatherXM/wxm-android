package com.weatherxm.ui.managesubscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.Purchase
import com.weatherxm.R
import com.weatherxm.ui.components.compose.LargeText
import com.weatherxm.ui.components.compose.MediumText

@Suppress("FunctionNaming", "LongMethod")
@Preview
@Composable
fun CurrentPlanView(currentPurchase: Purchase? = null) {
    // TODO: STOPSHIP: Get the current plan and handle it accordingly
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
                verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_small))
            ) {
                if (currentPurchase == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LargeText(
                            text = stringResource(R.string.standard),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = colorResource(R.color.colorPrimary)
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.active),
                                color = colorResource(R.color.colorOnPrimary),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(
                                    horizontal = dimensionResource(R.dimen.padding_small_to_normal),
                                    vertical = dimensionResource(R.dimen.padding_extra_small)
                                )
                            )
                        }
                    }
                    MediumText(
                        text = stringResource(R.string.just_the_basics),
                        colorRes = R.color.darkGrey
                    )
                } else {
                    // TODO: STOPSHIP: Get the current plan and handle it accordingly
                }
            }
        }
    }
}
