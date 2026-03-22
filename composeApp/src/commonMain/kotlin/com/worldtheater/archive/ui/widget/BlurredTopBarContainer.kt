package com.worldtheater.archive.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.worldtheater.archive.ui.theme.TopBarHeight
import com.worldtheater.archive.ui.theme.rememberTopBarPadding

@Composable
fun BlurredTopBarContainer(
    modifier: Modifier = Modifier,
    contentColor: Color = contentColorFor(MaterialTheme.colorScheme.background),
    extraContentHeight: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .graphicsLayer {
                clip = false
            }
    ) {
        // 1. Background Layer (Always visible to provide readability mask)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to topBarBgA100(),
                        0.3f to topBarBgA90(),
                        0.7f to topBarBgA54(),
                        1.0f to Color.Transparent
                    )
                )
                .graphicsLayer {
                    this.alpha = 0.99f
                }
        )

        // 2. Content Layer
        val contentLayerModifier = Modifier.fillMaxWidth()

        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Box(
                modifier = contentLayerModifier
                    .graphicsLayer {
                        clip = false
                    }
            ) {
                val topBarPadding = rememberTopBarPadding()
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .height(TopBarHeight + topBarPadding + extraContentHeight)
                        .padding(horizontal = 12.dp)
                        .padding(top = topBarPadding)
                        .padding(bottom = 12.dp)
                ) {
                    content()
                }
            }
        }
    }
}
