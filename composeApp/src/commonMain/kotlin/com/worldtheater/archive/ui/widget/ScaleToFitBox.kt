package com.worldtheater.archive.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints

@Composable
fun ScaleToFitBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        // Measure the content with loose constraints to determine its ideal size
        val placeable =
            measurables.firstOrNull()
                ?.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))

        if (placeable != null) {
            val contentHeight = placeable.height
            val availableHeight = constraints.maxHeight

            // Calculate scale factor if content exceeds available height
            val scale = if (contentHeight > availableHeight && availableHeight > 0) {
                availableHeight.toFloat() / contentHeight.toFloat()
            } else {
                1f
            }

            // The component itself takes the size of the scaled content (or available space if we want to fill)
            // But since we want to center it usually, let's take available height or scaled height.
            // Actually, for TopBar title, we usually want it to fit within available height.
            // Width is also constrained.

            val width = placeable.width

            layout(
                width,
                if (availableHeight != Constraints.Infinity) availableHeight else contentHeight
            ) {
                // Determine layout width and height based on constraints
                // We center the content vertically if we have extra space, though ScaleToFit implies we fill if too big.

                // If we scale down, we need to place it such that it fits.
                // Using graphicsLayer scale would be easier but Layout allows us to affect the parent's perception of size.
                // However, scaling at drawing time is often cleaner for text.
                // But native Layout scaling isn't "native".
                // We typically use graphicsLayer to scale. But here we want the parent to know?

                // Actually, the easiest way to do "ScaleToFit" is placing the placeable with a graphicsLayer.
                // But `placeable.placeRelative` doesn't take scale.
                // We must use `placeable.placeRelativeWithLayer` which allows a layer block.

                val yOffset = (availableHeight - contentHeight) / 2f

                placeable.placeRelativeWithLayer(0, yOffset.toInt()) {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin.Center
                }
            }
        } else {
            layout(0, 0) {}
        }
    }
}
