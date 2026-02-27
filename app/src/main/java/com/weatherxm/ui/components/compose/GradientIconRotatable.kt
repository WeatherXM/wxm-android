package com.weatherxm.ui.components.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.weatherxm.R
import kotlin.math.min

/**
 * A reusable icon that can be rotated and tinted with either a gradient brush or a static color.
 * Useful for icons that need to be oriented based on data (e.g., wind direction).
 *
 * @param iconRes The drawable resource ID for the icon
 * @param rotation The rotation angle in degrees (clockwise)
 * @param modifier Modifier to be applied to the icon
 * @param size The size of the icon (default 14.dp)
 * @param brush Optional gradient brush to apply to the icon
 * @param tint Optional static color to apply to the icon (used if brush is null)
 */
@Suppress("LongMethod", "FunctionNaming", "LongParameterList")
@Composable
fun GradientIconRotatable(
    @DrawableRes iconRes: Int,
    rotation: Float,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    brush: Brush? = null,
    tint: Color? = null
) {
    val painter = painterResource(iconRes)

    if (brush != null) {
        // Use Canvas to apply gradient with rotation and proper masking using layer
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

                    // Translate to center position
                    translate(left, top) {
                        // Rotate around the center of the icon
                        rotate(
                            degrees = rotation,
                            pivot = androidx.compose.ui.geometry.Offset(
                                scaledWidth / 2,
                                scaledHeight / 2
                            )
                        ) {
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
                    }

                    restoreToCount(checkPoint)
                }
            }
        }
    } else {
        // Use standard Icon with solid color tint and rotation
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val intrinsicSize = painter.intrinsicSize
                val canvasSize = this.size

                val scale = min(
                    canvasSize.width / intrinsicSize.width,
                    canvasSize.height / intrinsicSize.height
                )

                val scaledWidth = intrinsicSize.width * scale
                val scaledHeight = intrinsicSize.height * scale

                val left = (canvasSize.width - scaledWidth) / 2
                val top = (canvasSize.height - scaledHeight) / 2

                translate(left, top) {
                    rotate(
                        degrees = rotation,
                        pivot = androidx.compose.ui.geometry.Offset(
                            scaledWidth / 2,
                            scaledHeight / 2
                        )
                    ) {
                        with(painter) {
                            draw(
                                size = Size(scaledWidth, scaledHeight),
                                alpha = 1f,
                                colorFilter = tint?.let {
                                    androidx.compose.ui.graphics.ColorFilter.tint(it)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Preview for RotatableGradientIcon with gradient brush at 45 degrees
 */
@Suppress("FunctionNaming", "MagicNumber")
@Preview(showBackground = true)
@Composable
fun PreviewRotatableGradientIconWithBrush() {
    GradientIconRotatable(
        iconRes = R.drawable.ic_wind_direction,
        rotation = 45f,
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
 * Preview for RotatableGradientIcon with static color at 90 degrees
 */
@Suppress("FunctionNaming", "MagicNumber")
@Preview(showBackground = true)
@Composable
fun PreviewRotatableGradientIconWithColor() {
    GradientIconRotatable(
        iconRes = R.drawable.ic_wind_direction,
        rotation = 90f,
        size = 24.dp,
        tint = Color.Gray
    )
}
