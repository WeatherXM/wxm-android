package com.weatherxm.ui.components.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.weatherxm.R
import kotlin.math.min

/**
 * A reusable icon composable that can be tinted with either a gradient brush or a static color.
 *
 * @param iconRes The drawable resource ID for the icon
 * @param modifier Modifier to be applied to the icon
 * @param size The size of the icon (default 14.dp)
 * @param brush Optional gradient brush to apply to the icon
 * @param tint Optional static color to apply to the icon (used if brush is null)
 */
@Suppress("FunctionNaming", "LongParameterList")
@Composable
fun GradientIcon(
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    brush: Brush? = null,
    tint: Color? = null
) {
    val painter = painterResource(iconRes)

    if (brush != null) {
        // Use Canvas to apply gradient with proper masking using layer
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                // Calculate the size that fits within the canvas while maintaining aspect ratio
                val intrinsicSize = painter.intrinsicSize
                val canvasSize = this.size
                
                val scale = min(
                    canvasSize.width / intrinsicSize.width,
                    canvasSize.height / intrinsicSize.height
                )
                
                val scaledWidth = intrinsicSize.width * scale
                val scaledHeight = intrinsicSize.height * scale
                
                // Center the icon
                val left = (canvasSize.width - scaledWidth) / 2
                val top = (canvasSize.height - scaledHeight) / 2
                
                // Use saveLayer to isolate the blend mode operations
                drawContext.canvas.nativeCanvas.apply {
                    val checkPoint = saveLayer(null, null)
                    
                    // Translate to center position and draw
                    translate(left, top) {
                        // Step 1: Draw the icon (this becomes our mask)
                        with(painter) {
                            draw(
                                size = Size(scaledWidth, scaledHeight),
                                alpha = 1f
                            )
                        }
                        
                        // Step 2: Draw gradient with SrcIn blend mode
                        // This makes gradient only visible where icon pixels exist
                        drawRect(
                            brush = brush,
                            size = Size(scaledWidth, scaledHeight),
                            blendMode = BlendMode.SrcIn
                        )
                    }
                    
                    restoreToCount(checkPoint)
                }
            }
        }
    } else {
        // Use standard Icon with solid color tint
        Icon(
            painter = painter,
            contentDescription = null,
            modifier = modifier.size(size),
            tint = tint ?: Color.Unspecified
        )
    }
}

/**
 * Preview for GradientIcon with gradient brush
 */
@Suppress("FunctionNaming", "MagicNumber")
@Preview(showBackground = true)
@Composable
fun PreviewGradientIconWithBrush() {
    GradientIcon(
        iconRes = R.drawable.ic_weather_precip_probability,
        size = 24.dp,
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF2196F3),
                Color(0xFFE91E63)
            )
        )
    )
}

/**
 * Preview for GradientIcon with static color
 */
@Suppress("FunctionNaming")
@Preview(showBackground = true)
@Composable
fun PreviewGradientIconWithColor() {
    GradientIcon(
        iconRes = R.drawable.ic_weather_precip_probability,
        size = 24.dp,
        tint = Color.Gray
    )
}
