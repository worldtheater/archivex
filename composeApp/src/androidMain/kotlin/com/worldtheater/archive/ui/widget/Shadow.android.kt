package com.worldtheater.archive.ui.widget

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
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
        .graphicsLayer {
            clip = false
        }
        .drawBehind {
            drawIntoCanvas { canvas ->
                val paint = Paint()
                val frameworkPaint = paint.asFrameworkPaint()
                frameworkPaint.color = android.graphics.Color.TRANSPARENT
                frameworkPaint.setShadowLayer(
                    shadowRadius.toPx(),
                    shadowOffsetX.toPx(),
                    shadowOffsetY.toPx(),
                    shadowColor.toArgb()
                )

                val rx = if (isPill) size.height / 2f else cornerRadius.toPx()
                val ry = rx

                canvas.drawRoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    radiusX = rx,
                    radiusY = ry,
                    paint = paint
                )
            }
        }
}
