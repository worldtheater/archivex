package com.worldtheater.archive.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.worldtheater.archive.ui.shape.SmoothCapsuleShape
import com.worldtheater.archive.ui.theme.DefaultAppBarButtonSize
import kotlin.math.roundToInt

@Composable
fun CapsuleContainer(
    modifier: Modifier = Modifier,
    backgroundColor: Color = capsuleBgColor(),
    contentColor: Color = capsuleContentColor(),
    isCircle: Boolean = false, // Added to support circular icons
    content: @Composable () -> Unit
) {
    val containerModifier = if (isCircle) {
        modifier.size(DefaultAppBarButtonSize)
    } else {
        modifier.height(DefaultAppBarButtonSize)
    }

    // Use SquircleShape for continuous curvature only if not a perfect circle
    val shape = if (isCircle) {
        RoundedCornerShape(50)
    } else {
        SmoothCapsuleShape()
    }

    Box(
        modifier = containerModifier
            .graphicsLayer {
                clip = false
            }
            .offsetShadow(
                isPill = true
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Box(
                modifier = (if (isCircle) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.fillMaxHeight()
                })
                    .background(backgroundColor, shape)
                    .then(
                        if (isCircle) {
                            Modifier
                        } else {
                            Modifier.padding(horizontal = 20.dp)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCircle) {
                    content()
                } else {
                    Layout(
                        content = { Box { content() } }
                    ) { measurables, constraints ->
                        val placeable = measurables.first()
                            .measure(
                                constraints.copy(
                                    minHeight = 0,
                                    maxHeight = Constraints.Infinity
                                )
                            )

                        val contentHeight = placeable.height.toFloat()
                        val availableHeight = constraints.maxHeight.toFloat()

                        val scale = if (contentHeight > availableHeight && availableHeight > 0) {
                            availableHeight / contentHeight
                        } else {
                            1f
                        }

                        val scaledWidth = (placeable.width * scale).roundToInt()
                        val scaledHeight = (placeable.height * scale).roundToInt()

                        layout(scaledWidth, scaledHeight) {
                            val x = (scaledWidth - placeable.width) / 2
                            val y = (scaledHeight - placeable.height) / 2

                            placeable.placeRelativeWithLayer(x, y) {
                                scaleX = scale
                                scaleY = scale
                            }
                        }
                    }
                }
            }
        }
    }
}
