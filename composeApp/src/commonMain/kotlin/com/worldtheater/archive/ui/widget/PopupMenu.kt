package com.worldtheater.archive.ui.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.worldtheater.archive.ui.shape.SmoothRoundedCornerShape

enum class PopupMenuHorizontalAlign {
    CENTER, START, END
}

enum class PopupMenuVerticalAlign {
    AUTO, TOP, BOTTOM
}

@Composable
fun PopupMenu(
    menuItems: Map<String, () -> Unit>,
    showMenu: Boolean,
    onDismiss: () -> Unit,
    anchorPosition: IntOffset = IntOffset.Zero,
    minWidth: Dp = 84.dp,
    maxWidth: Dp = 144.dp,
    anchorVerticalSpacing: Dp = 24.dp,
    horizontalAlign: PopupMenuHorizontalAlign = PopupMenuHorizontalAlign.CENTER,
    verticalAlign: PopupMenuVerticalAlign = PopupMenuVerticalAlign.AUTO,
) {
    val expandedStates = remember { MutableTransitionState(false) }
    expandedStates.targetState = showMenu

    if (expandedStates.currentState || expandedStates.targetState) {
        val density = LocalDensity.current
        val shadowPadding = 24.dp
        val windowPadding = 8.dp
        val anchorGap = anchorVerticalSpacing
        val shadowPaddingPx = with(density) { shadowPadding.roundToPx() }
        val windowPaddingPx = with(density) { windowPadding.roundToPx() }
        val anchorGapPx = with(density) { anchorGap.roundToPx() }

        val popupPositionProvider = remember(
            anchorPosition,
            shadowPaddingPx,
            windowPaddingPx,
            anchorGapPx,
            horizontalAlign,
            verticalAlign
        ) {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset {
                    val visualWidth =
                        (popupContentSize.width - shadowPaddingPx * 2).coerceAtLeast(0)
                    val visualHeight =
                        (popupContentSize.height - shadowPaddingPx * 2).coerceAtLeast(0)
                    val resolvedAnchorX = anchorBounds.left + anchorPosition.x
                    val resolvedAnchorY = anchorBounds.top + anchorPosition.y

                    val minLeft = windowPaddingPx
                    val maxLeft =
                        (windowSize.width - windowPaddingPx - visualWidth).coerceAtLeast(minLeft)
                    val preferredLeft = when (horizontalAlign) {
                        PopupMenuHorizontalAlign.CENTER -> resolvedAnchorX - visualWidth / 2
                        PopupMenuHorizontalAlign.START -> resolvedAnchorX
                        PopupMenuHorizontalAlign.END -> resolvedAnchorX - visualWidth
                    }
                    val visualLeft = preferredLeft.coerceIn(minLeft, maxLeft)

                    val minTop = 0 // we dont need top window padding
                    val maxTop =
                        (windowSize.height - windowPaddingPx - visualHeight).coerceAtLeast(minTop)
                    val belowTop = resolvedAnchorY + anchorGapPx
                    val aboveTop = resolvedAnchorY - anchorGapPx - visualHeight
                    val preferAbove = resolvedAnchorY > (windowSize.height * 2 / 3)

                    val visualTop = when (verticalAlign) {
                        PopupMenuVerticalAlign.TOP -> resolvedAnchorY
                        PopupMenuVerticalAlign.BOTTOM -> resolvedAnchorY - visualHeight
                        PopupMenuVerticalAlign.AUTO -> when {
                            preferAbove && aboveTop >= windowPaddingPx -> aboveTop
                            !preferAbove && belowTop + visualHeight <= windowSize.height - windowPaddingPx -> belowTop
                            belowTop + visualHeight <= windowSize.height - windowPaddingPx -> belowTop
                            aboveTop >= windowPaddingPx -> aboveTop
                            else -> belowTop.coerceIn(minTop, maxTop)
                        }
                    }
                    val clampedTop = visualTop.coerceIn(minTop, maxTop)

                    return IntOffset(
                        x = visualLeft - shadowPaddingPx,
                        y = clampedTop - shadowPaddingPx
                    )
                }
            }
        }

        Popup(
            onDismissRequest = onDismiss,
            popupPositionProvider = popupPositionProvider,
            properties = PopupProperties(focusable = true, clippingEnabled = false)
        ) {
            AnimatedVisibility(
                visibleState = expandedStates,
                enter = fadeIn(animationSpec = tween(durationMillis = 150)),
                exit = fadeOut(animationSpec = tween(durationMillis = 150))
            ) {
                Box(
                    modifier = Modifier
                        .padding(shadowPadding)
                        .offsetShadow(
                            shadowRadius = 28.dp,
                            cornerRadius = 28.dp,
                            shadowOffsetY = 4.dp
                        )
                        .clip(SmoothRoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .widthIn(min = minWidth, max = maxWidth)
                ) {
                    Column {
                        menuItems.forEach { (text, action) ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        action()
                                        onDismiss()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = text,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
