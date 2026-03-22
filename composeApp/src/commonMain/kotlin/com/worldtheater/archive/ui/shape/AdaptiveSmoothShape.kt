package com.worldtheater.archive.ui.shape

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A shape that adapts its appearance based on the component's height.
 *
 * - If the height is less than [limitHeight], it renders as a [SmoothCapsuleShape] (pill).
 * - If the height is greater than or equal to [limitHeight], it renders as a
 *   [SmoothRoundedCornerShape] with the specified [cornerRadius].
 *
 * This is useful for components like tags that should look like pills when single-line (short),
 * but look like standard rounded rectangles when multi-line (tall) to avoid excessive curvature.
 */
class AdaptiveSmoothShape(
    private val cornerRadius: Dp = 16.dp,
    private val limitHeight: Dp = 40.dp
) : Shape {

    private val capsuleShape = SmoothCapsuleShape()
    private val roundedCornerShape =
        SmoothRoundedCornerShape(cornerRadius)

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val limitPx = with(density) { limitHeight.toPx() }

        return if (size.height < limitPx) {
            capsuleShape.createOutline(size, layoutDirection, density)
        } else {
            // Re-create generic rounded corner shape if we wanted dynamic radius, 
            // but here we use the pre-created one for the fixed radius strategy.
            roundedCornerShape.createOutline(size, layoutDirection, density)
        }
    }
}
