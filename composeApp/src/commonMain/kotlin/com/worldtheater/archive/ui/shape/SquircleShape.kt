package com.worldtheater.archive.ui.shape

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * 仿 iOS 连续曲率圆角 (Squircle / Continuous Corner)
 * 核心逻辑：使用立方的贝塞尔曲线代替标准圆弧，实现 G2 连续性。
 */
class SquircleShape(
    private val cornerRadius: Dp
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radiusPx = with(density) { cornerRadius.toPx() }
        // 限制半径不超过宽高的一半
        val r = radiusPx.coerceAtMost(size.minDimension / 2)

        // iOS 的连续曲率圆角通常比标准圆角占用更多的边缘长度
        // 约为 1.28 倍 (这是一个经验值，用于近似 iOS 的 smooth corner)
        val smoothFactor = 1.28f
        val offset = r * smoothFactor

        val path = Path().apply {
            val w = size.width
            val h = size.height

            // 左上角
            moveTo(0f, offset)
            cubicTo(0f, r, 0f, 0f, offset, 0f)

            // 上边
            lineTo(w - offset, 0f)

            // 右上角
            cubicTo(w - r, 0f, w, 0f, w, offset)

            // 右边
            lineTo(w, h - offset)

            // 右下角
            cubicTo(w, h - r, w, h, w - offset, h)

            // 下边
            lineTo(offset, h)

            // 左下角
            cubicTo(r, h, 0f, h, 0f, h - offset)

            close()
        }

        return Outline.Generic(path)
    }
}
