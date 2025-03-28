package com.weatherxm.ui.components.compose

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.weatherxm.R

@Suppress("FunctionNaming", "LongParameterList", "MagicNumber")
@Composable
fun AnnouncementBannerView(
    title: String,
    subtitle: String,
    actionLabel: String,
    showActionButton: Boolean,
    showCloseButton: Boolean,
    onAction: (() -> Unit)?,
    onClose: (() -> Unit)?,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large))
    ) {
        Column(
            modifier = Modifier
                .paint(
                    painter = painterResource(R.drawable.gradient_background),
                    contentScale = ContentScale.Crop
                )
                .padding(dimensionResource(R.dimen.padding_normal_to_large)),
            verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_small))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.light_layer1),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(if (showCloseButton) 0.8F else 1F)
                )
                if (showCloseButton) {
                    IconButton(
                        onClick = { onClose?.invoke() },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = colorResource(R.color.light_layer1)
                        ),
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.action_close)
                        )
                    }
                }
            }
            if (subtitle.isNotEmpty()) {
                MediumText(subtitle, colorRes = R.color.light_layer2)
            }
            if (showActionButton) {
                Button(
                    onClick = { onAction?.invoke() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.translucent_light_layer1),
                        contentColor = colorResource(R.color.light_layer1)
                    )
                ) {
                    LargeText(
                        actionLabel,
                        fontWeight = FontWeight.Bold,
                        colorRes = R.color.light_layer1
                    )
                }
            }
        }
    }
}

@Suppress("FunctionNaming")
@Preview
@Composable
fun PreviewAnnouncementBanner() {
    AnnouncementBannerView(
        title = "Upgrade to WeatherXM Pro!",
        subtitle = "Get exclusive Pro features, better forecasts, more data, and advanced tools. " +
            "Take your weather insights to the next level.",
        actionLabel = "Explore Pro Features",
        showActionButton = true,
        showCloseButton = true,
        onAction = {},
        onClose = {}
    )
}
