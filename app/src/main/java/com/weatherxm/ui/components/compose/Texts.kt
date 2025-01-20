package com.weatherxm.ui.components.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.weatherxm.R

@Suppress("FunctionNaming")
@Composable
fun MediumText(
    text: String,
    fontWeight: FontWeight = FontWeight.Normal,
    colorRes: Int = R.color.colorOnSurface
) {
    Text(
        text = text,
        fontWeight = fontWeight,
        color = colorResource(colorRes),
        style = MaterialTheme.typography.bodyMedium
    )
}

@Suppress("FunctionNaming")
@Composable
fun LargeText(
    text: String,
    fontWeight: FontWeight = FontWeight.Normal,
    fontSize: TextUnit = 16.sp,
    colorRes: Int = R.color.colorOnSurface
) {
    Text(
        text = text,
        fontWeight = fontWeight,
        fontSize = fontSize,
        color = colorResource(colorRes),
        style = MaterialTheme.typography.bodyLarge
    )
}

@Suppress("FunctionNaming")
@Composable
fun Title(
    text: String,
    fontSize: TextUnit = 24.sp,
    colorRes: Int = R.color.colorOnSurface
) {
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = FontWeight.SemiBold,
        color = colorResource(colorRes),
        style = MaterialTheme.typography.headlineSmall
    )
}
