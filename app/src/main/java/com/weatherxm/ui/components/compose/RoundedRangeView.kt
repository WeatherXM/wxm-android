package com.weatherxm.ui.components.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.weatherxm.R

@Suppress("FunctionNaming")
@Composable
fun RoundedRangeView(
    height: Dp,
    currentRange: ClosedFloatingPointRange<Float>,
    totalRange: ClosedFloatingPointRange<Float>,
    inactiveColorResId: Int,
    activeColorResId: Int,
    activeColorBrush: Brush? = null
) {
    val cornerRadius = dimensionResource(R.dimen.radius_extra_extra_large).value
    val inactiveColor = colorResource(inactiveColorResId)
    val activeColor = colorResource(activeColorResId)

    Canvas(modifier = Modifier.height(height)) {
        val width = size.width
        val trackHeight = size.height

        // Draw inactive track
        drawRoundRect(
            color = inactiveColor,
            size = Size(width, trackHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )

        // Calculate active range position
        val totalSpan = totalRange.endInclusive - totalRange.start
        val startFraction = (currentRange.start - totalRange.start) / totalSpan
        val endFraction = (currentRange.endInclusive - totalRange.start) / totalSpan

        val startX = width * startFraction
        val activeWidth = width * (endFraction - startFraction)

        // Draw active track
        if (activeColorBrush != null) {
            drawRoundRect(
                brush = activeColorBrush,
                topLeft = Offset(startX, 0f),
                size = Size(activeWidth, trackHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        } else {
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(startX, 0f),
                size = Size(activeWidth, trackHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        }
    }
}

@Suppress("FunctionNaming", "MagicNumber")
@Preview
@Composable
fun PreviewRoundedRangeView() {
    RoundedRangeView(16.dp, 0F..25F, 0F..100F, R.color.colorBackground, R.color.crypto)
}
