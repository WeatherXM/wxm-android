package com.weatherxm.ui.components.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.weatherxm.R

@Suppress("FunctionNaming", "MagicNumber")
@Preview
@Composable
fun GradientButton(
    textRes: Int = R.string.ai_health_check,
    iconRes: Int = R.drawable.ic_update,
    onClick: () -> Unit = {}
) {
    Button(
        modifier = Modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(231, 150, 175),
                        Color(187, 136, 228),
                        Color(131, 182, 220),
                        Color(105, 117, 215)
                    )
                ),
                shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large))
            )
            .height(30.dp),
        onClick = onClick,
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R.dimen.padding_small),
            vertical = dimensionResource(R.dimen.padding_extra_small)
        ),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
    ) {
        Row(horizontalArrangement = spacedBy(dimensionResource(R.dimen.padding_extra_small))) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = colorResource(R.color.dark_text)
            )
            SmallText(
                text = stringResource(textRes),
                fontWeight = FontWeight.Bold,
                colorRes = R.color.dark_text
            )
        }
    }
}
