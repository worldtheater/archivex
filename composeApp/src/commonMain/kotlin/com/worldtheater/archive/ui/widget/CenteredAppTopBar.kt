package com.worldtheater.archive.ui.widget

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CenteredAppTopBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = {},
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    centerContent: (@Composable () -> Unit)? = null,
    centerContentVisible: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(backgroundColor),
) {
    BlurredTopBarContainer(
        modifier = modifier,
        contentColor = contentColor,
    ) {
        CenteredAppTopBarContent(
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            centerContent = centerContent,
            centerContentVisible = centerContentVisible,
        )
    }
}

@Composable
fun BoxScope.CenteredAppTopBarContent(
    title: (@Composable () -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    centerContent: (@Composable () -> Unit)? = null,
    centerContentVisible: Boolean = true,
    isFloating: Boolean = true,
    elementBackgroundColor: Color? = null,
    elementContentColor: Color? = null
) {
    val alpha by animateFloatAsState(
        targetValue = if (centerContentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 200)
    )

    val translateY by animateDpAsState(
        targetValue = if (centerContentVisible) 0.dp else (-15).dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMedium,
            visibilityThreshold = Dp.VisibilityThreshold
        )
    )

    val elementBgColor =
        if (isFloating) elementBackgroundColor ?: capsuleBgColor() else Color.Transparent
    val elementFgColor = elementContentColor ?: capsuleContentColor()

    Row(
        modifier = Modifier.align(Alignment.CenterStart),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (navigationIcon != null) {
            CapsuleContainer(
                backgroundColor = elementBgColor,
                contentColor = elementFgColor,
                isCircle = true
            ) {
                navigationIcon()
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        if (centerContent == null && title != null) {
            CapsuleContainer(
                backgroundColor = elementBgColor,
                contentColor = elementFgColor,
                isCircle = false
            ) {
                title()
            }
        }
    }

    if (centerContent != null) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 48.dp)
                .graphicsLayer {
                    this.alpha = alpha
                    this.translationY = translateY.toPx()
                }
        ) {
            centerContent()
        }
    }

    if (actions != null) {
        CapsuleContainer(
            modifier = Modifier.align(Alignment.CenterEnd),
            backgroundColor = elementBgColor,
            contentColor = elementFgColor,
            isCircle = true
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                actions()
            }
        }
    }
}
