package com.worldtheater.archive.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val TopBarHeight = 60.dp
val DefaultStatusBarHeight = defaultStatusBarHeight()
val DefaultAppBarButtonSize = 48.dp

expect fun defaultStatusBarHeight(): Dp

@Composable
fun rememberTopBarPadding(): Dp {
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    if (topPadding >= DefaultStatusBarHeight) {
        return 0.dp
    } else {
        return DefaultStatusBarHeight - topPadding
    }
}

@Composable
fun rememberContentTopPadding(): Dp {
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val extraPadding = if (topPadding >= DefaultStatusBarHeight) {
        0.dp
    } else {
        DefaultStatusBarHeight - topPadding
    }
    return TopBarHeight + extraPadding + (topPadding.takeIf { it > 0.dp } ?: DefaultStatusBarHeight)
}
