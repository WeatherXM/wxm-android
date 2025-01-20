package com.weatherxm.ui.components.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
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
