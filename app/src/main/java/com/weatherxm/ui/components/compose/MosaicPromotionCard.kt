package com.weatherxm.ui.components.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherxm.R

@Suppress("FunctionNaming")
@Composable
fun MosaicPromotionCard(hasFreeSubAvailable: Boolean, onClickListener: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.blueTint)
        ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = dimensionResource(R.dimen.elevation_normal)
        ),
        border = BorderStroke(2.dp, colorResource(R.color.dark_crypto_opacity_30))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_normal_to_large)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.mosaic_forecast).uppercase(),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorResource(R.color.colorPrimary),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                modifier = Modifier.padding(top = dimensionResource(R.dimen.padding_small)),
                text = stringResource(R.string.mosaic_prompt_tagline),
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.colorOnSurface),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                modifier = Modifier.padding(top = dimensionResource(R.dimen.padding_normal)),
                text = stringResource(R.string.mosaic_prompt_explanation),
                color = colorResource(R.color.chart_primary_line),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = dimensionResource(R.dimen.padding_normal_to_large),
                        start = dimensionResource(R.dimen.padding_normal),
                        end = dimensionResource(R.dimen.padding_normal)
                    ),
                onClick = { onClickListener() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.colorPrimary),
                    contentColor = colorResource(R.color.colorOnPrimary)
                ),
                shape = RoundedCornerShape(dimensionResource(R.dimen.radius_extra_extra_large)),
                contentPadding = PaddingValues(
                    horizontal = 40.dp,
                    vertical = dimensionResource(R.dimen.padding_normal)
                )
            ) {
                LargeText(
                    text = stringResource(R.string.upgrade_to_premium),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    colorRes = R.color.colorOnPrimary
                )
            }
            if (hasFreeSubAvailable) {
                Text(
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.padding_small)),
                    text = stringResource(R.string.free_subscription_claim),
                    color = colorResource(R.color.chart_primary_line),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Suppress("FunctionNaming")
@Preview
@Composable
fun PreviewMosaicPromotionCard() {
    MosaicPromotionCard(true) {
        // Do nothing
    }
}
