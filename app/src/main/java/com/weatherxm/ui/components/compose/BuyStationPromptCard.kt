package com.weatherxm.ui.components.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.sp
import com.weatherxm.R

@Preview
@Suppress("FunctionNaming", "LongMethod")
@Composable
fun BuyStationPromptCard(onBuyStation: () -> Unit = {}) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.blueTint)
        ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = dimensionResource(R.dimen.elevation_normal)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_normal)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_small))
        ) {
            Column(
                modifier = Modifier.weight(1F),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_normal))
            ) {
                LargeText(
                    text = stringResource(R.string.enter_weather_3_0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Row(
                    modifier = Modifier.padding(start = dimensionResource(R.dimen.margin_small)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_checkmark_only),
                        contentDescription = null,
                        tint = colorResource(R.color.success)
                    )
                    MediumText(
                        stringResource(R.string.deploy_your_station),
                        paddingValues = PaddingValues(
                            start = dimensionResource(R.dimen.margin_small)
                        )
                    )
                }
                Row(
                    modifier = Modifier.padding(start = dimensionResource(R.dimen.margin_small)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_checkmark_only),
                        contentDescription = null,
                        tint = colorResource(R.color.success)
                    )
                    MediumText(
                        stringResource(R.string.check_the_weather),
                        paddingValues = PaddingValues(
                            start = dimensionResource(R.dimen.margin_small)
                        )
                    )
                }
                Row(
                    modifier = Modifier.padding(start = dimensionResource(R.dimen.margin_small)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_checkmark_only),
                        contentDescription = null,
                        tint = colorResource(R.color.success)
                    )
                    MediumText(
                        stringResource(R.string.earn_wxm_tokens),
                        paddingValues = PaddingValues(
                            start = dimensionResource(R.dimen.margin_small)
                        )
                    )
                }
                Button(
                    onClick = { onBuyStation() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.colorPrimary),
                        contentColor = colorResource(R.color.colorBackground)
                    ),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium)),
                ) {
                    MediumText(
                        stringResource(R.string.action_buy_station),
                        fontWeight = FontWeight.Bold,
                        colorRes = R.color.colorBackground
                    )
                }
            }
            Image(
                modifier = Modifier.weight(1F),
                painter = painterResource(R.drawable.network_stats_weather_station),
                contentDescription = null
            )
        }
    }
}
