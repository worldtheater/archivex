package com.worldtheater.archive.ui.widget

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldtheater.archive.ui.theme.WIDGET_OFFSET_SHADOW_COLOR

expect fun Modifier.offsetShadow(
    shadowColor: Color = WIDGET_OFFSET_SHADOW_COLOR,
    shadowRadius: Dp = 24.dp,
    shadowOffsetX: Dp = 0.dp,
    shadowOffsetY: Dp = 8.dp,
    cornerRadius: Dp = 24.dp,
    isPill: Boolean = false
): Modifier
