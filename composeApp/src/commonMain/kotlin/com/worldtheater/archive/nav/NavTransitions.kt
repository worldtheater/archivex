package com.worldtheater.archive.nav

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntOffset
import com.worldtheater.archive.nav.AppNavAnimationPreset.*

internal fun androidEnterTransitionFor(preset: AppNavAnimationPreset) = when (preset) {
    SLIDE_IN_FROM_RIGHT -> slideInHorizontally(initialOffsetX = { it })
    SLIDE_IN_FROM_BOTTOM -> slideInVertically(
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold
        ),
        initialOffsetY = { it }
    )

    FADE_SCALE_IN -> fadeIn(
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = 0.001f
        ),
        initialAlpha = 0.7f
    ) + scaleIn(
        initialScale = 0.92f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = 0.001f
        )
    )

    SLIDE_LEFT_FADE_IN -> slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn(initialAlpha = 0.7f)
    else -> fadeIn(initialAlpha = 0.7f)
}

internal fun androidExitTransitionFor(preset: AppNavAnimationPreset) = when (preset) {
    SLIDE_OUT_TO_RIGHT -> slideOutHorizontally(targetOffsetX = { it })
    SLIDE_OUT_TO_BOTTOM -> slideOutVertically(
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold
        ),
        targetOffsetY = { it }
    )

    FADE_SCALE_OUT -> fadeOut(
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        targetAlpha = 0.7f
    ) + scaleOut(
        targetScale = 0.92f,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    SLIDE_LEFT_FADE_OUT -> slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut(targetAlpha = 0.7f)
    else -> fadeOut(targetAlpha = 0.7f)
}
