package com.weatherxm.ui.components.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.weatherxm.R

@Suppress("FunctionNaming")
@Composable
fun ProPromotionCard(subtitleResId: Int, onClickListener: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.blueTint)
        ),
        onClick = { onClickListener() },
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = dimensionResource(R.dimen.elevation_normal)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = dimensionResource(R.dimen.padding_normal_to_large),
                    horizontal = dimensionResource(R.dimen.padding_normal)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = dimensionResource(R.dimen.padding_small)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.padding_small)
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_diamond),
                    contentDescription = null,
                    tint = colorResource(R.color.textColor)
                )
                Column {
                    LargeText(
                        stringResource(R.string.weatherxm_pro),
                        fontWeight = FontWeight.Bold,
                        colorRes = R.color.colorPrimary
                    )
                    MediumText(stringResource(subtitleResId))
                }
            }
            Button(
                onClick = { onClickListener() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.colorPrimary),
                    contentColor = colorResource(R.color.colorBackground)
                ),
                shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium)),
            ) {
                MediumText(
                    stringResource(R.string.action_get_pro),
                    fontWeight = FontWeight.Bold,
                    colorRes = R.color.colorBackground
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewProPromotionCard() {
    ProPromotionCard(R.string.take_weather_insights_next_level) {
        // Do nothing
    }
}
