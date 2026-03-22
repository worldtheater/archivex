package com.worldtheater.archive.ui.widget

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

actual fun Modifier.offsetShadow(
    shadowColor: Color,
    shadowRadius: Dp,
    shadowOffsetX: Dp,
    shadowOffsetY: Dp,
    cornerRadius: Dp,
    isPill: Boolean
): Modifier {
    return this
        .graphicsLayer { clip = false }
        .drawBehind {
            val dx = shadowOffsetX.toPx()
            val dy = shadowOffsetY.toPx()
            val radiusPx = shadowRadius.toPx().coerceAtLeast(1f)
            val baseCorner = if (isPill) size.height / 2f else cornerRadius.toPx()

            // Desktop fallback: sample many low-alpha layers to mimic a smooth gaussian shadow.
            // More samples reduce visible banding/rings compared with coarse 2-3 layer stacking.
            val sampleCount = 14
            for (i in 1..sampleCount) {
                val t = i / sampleCount.toFloat()
                val spread = radiusPx * (0.08f + 1.12f * t)
                val extraDy = dy * (0.35f + 0.95f * t)
                val alphaScale = 0.085f * (1f - t) * (1f - t)
                val left = dx - spread
                val top = dy + extraDy - spread
                val width = size.width + spread * 2f
                val height = size.height + spread * 2f
                val corner = if (isPill) height / 2f else baseCorner + spread
                drawRoundRect(
                    color = shadowColor.copy(alpha = (shadowColor.alpha * alphaScale).coerceIn(0f, 1f)),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(width, height),
                    cornerRadius = CornerRadius(corner, corner)
                )
            }
        }
}
