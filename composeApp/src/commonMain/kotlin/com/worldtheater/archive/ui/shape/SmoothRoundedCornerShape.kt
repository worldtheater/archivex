package com.worldtheater.archive.ui.shape

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.min

/**
 * A smooth rounded corner shape using quadratic Bezier curves.
 * Inherits from [CornerBasedShape] to support usage in MaterialTheme.shapes.
 */
class SmoothRoundedCornerShape(
    topStartCorner: CornerSize,
    topEndCorner: CornerSize,
    bottomEndCorner: CornerSize,
    bottomStartCorner: CornerSize
) : CornerBasedShape(topStartCorner, topEndCorner, bottomEndCorner, bottomStartCorner) {

    constructor(radius: Dp) : this(
        CornerSize(radius),
        CornerSize(radius),
        CornerSize(radius),
        CornerSize(radius)
    )

    constructor(percent: Int) : this(
        CornerSize(percent),
        CornerSize(percent),
        CornerSize(percent),
        CornerSize(percent)
    )

    // Constants
    private val controlPointOffset = 0.585786f
    private val roundJointOffset = 0.292893f
    private val smoothFactor = 1.175f

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize
    ): CornerBasedShape {
        return SmoothRoundedCornerShape(
            topStart,
            topEnd,
            bottomEnd,
            bottomStart
        )
    }

    // Override the abstract method from CornerBasedShape which provides resolved corner sizes
    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection
    ): Outline {
        val w = size.width
        val h = size.height

        // Limit radii to prevent overlapping (simple approach: max radius = min(w, h)/2)
        val maxR = min(w, h) / 2f

        val tl = if (topStart * smoothFactor > maxR) maxR / smoothFactor else topStart
        val tr = if (topEnd * smoothFactor > maxR) maxR / smoothFactor else topEnd
        val br = if (bottomEnd * smoothFactor > maxR) maxR / smoothFactor else bottomEnd
        val bl = if (bottomStart * smoothFactor > maxR) maxR / smoothFactor else bottomStart

        val path = Path()

        // Top-Left
        val tlSmooth = tl * smoothFactor
        val tlCp = tl * controlPointOffset
        val tlCut = tl * roundJointOffset

        path.moveTo(0f, tlSmooth)
        path.quadraticTo(0f, tlCp, tlCut, tlCut) // Vertical -> 45
        path.quadraticTo(tlCp, 0f, tlSmooth, 0f) // 45 -> Horizontal

        // Top Line
        val trSmooth = tr * smoothFactor
        path.lineTo(w - trSmooth, 0f)

        // Top-Right
        val trCp = tr * controlPointOffset
        val trCut = tr * roundJointOffset

        path.quadraticTo(w - trCp, 0f, w - trCut, trCut) // Horizontal -> 45
        path.quadraticTo(w, trCp, w, trSmooth) // 45 -> Vertical

        // Right Line
        val brSmooth = br * smoothFactor
        path.lineTo(w, h - brSmooth)

        // Bottom-Right
        val brCp = br * controlPointOffset
        val brCut = br * roundJointOffset

        path.quadraticTo(w, h - brCp, w - brCut, h - brCut) // Vertical -> 45
        path.quadraticTo(w - brCp, h, w - brSmooth, h) // 45 -> Horizontal

        // Bottom Line
        val blSmooth = bl * smoothFactor
        path.lineTo(blSmooth, h)

        // Bottom-Left
        val blCp = bl * controlPointOffset
        val blCut = bl * roundJointOffset

        path.quadraticTo(blCp, h, blCut, h - blCut) // Horizontal -> 45
        path.quadraticTo(0f, h - blCp, 0f, h - blSmooth) // 45 -> Vertical

        path.close()

        return Outline.Generic(path)
    }
}
