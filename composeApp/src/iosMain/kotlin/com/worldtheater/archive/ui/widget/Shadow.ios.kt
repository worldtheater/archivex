package com.worldtheater.archive.ui.widget

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

            // Compose iOS doesn't expose native shadow blur here, so approximate it
            // with two components:
            // 1. a broad ambient shadow that gives overall depth
            // 2. a very soft contact shadow slightly below the surface
            // Keep both detached from the edge so the shadow reads as depth, not outline.
            val ambientSpread = radiusPx * 1.55f
            val ambientTop = dy * 0.55f - ambientSpread
            val ambientLeft = dx - ambientSpread
            val ambientWidth = size.width + ambientSpread * 2f
            val ambientHeight = size.height + ambientSpread * 2f
            val ambientCorner = if (isPill) ambientHeight / 2f else baseCorner + ambientSpread

            drawRoundRect(
                color = shadowColor.copy(alpha = (shadowColor.alpha * 0.07f).coerceIn(0f, 1f)),
                topLeft = Offset(ambientLeft, ambientTop),
                size = Size(ambientWidth, ambientHeight),
                cornerRadius = CornerRadius(ambientCorner, ambientCorner)
            )

            val contactSpread = radiusPx * 0.72f
            val contactTop = dy * 0.9f + radiusPx * 0.18f - contactSpread
            val contactLeft = dx - contactSpread
            val contactWidth = size.width + contactSpread * 2f
            val contactHeight = size.height + contactSpread * 2f
            val contactCorner = if (isPill) contactHeight / 2f else baseCorner + contactSpread

            drawRoundRect(
                color = shadowColor.copy(alpha = (shadowColor.alpha * 0.045f).coerceIn(0f, 1f)),
                topLeft = Offset(contactLeft, contactTop),
                size = Size(contactWidth, contactHeight),
                cornerRadius = CornerRadius(contactCorner, contactCorner)
            )

            val sampleCount = 24
            for (i in 1..sampleCount) {
                val t = i / sampleCount.toFloat()
                val easedT = t * t
                val spread = radiusPx * (0.56f + 1.2f * easedT)
                val extraDy = dy * (0.42f + 0.88f * t)
                val alphaScale = 0.038f * (1f - t) * (1f - t) * (1f - 0.18f * t)
                val left = dx - spread
                val top = dy + extraDy - spread
                val width = size.width + spread * 2f
                val height = size.height + spread * 2f
                val corner = if (isPill) height / 2f else baseCorner + spread

                drawRoundRect(
                    color = shadowColor.copy(
                        alpha = (shadowColor.alpha * alphaScale).coerceIn(0f, 1f)
                    ),
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(corner, corner)
                )
            }
        }
}
