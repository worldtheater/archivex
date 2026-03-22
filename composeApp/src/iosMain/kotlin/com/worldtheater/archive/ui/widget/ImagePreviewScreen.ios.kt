package com.worldtheater.archive.ui.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import archivex.composeapp.generated.resources.Res
import archivex.composeapp.generated.resources.action_cancel
import com.worldtheater.archive.platform.system.AppBackHandler
import com.worldtheater.archive.ui.theme.DefaultAppBarButtonSize
import com.worldtheater.archive.ui.theme.isAppInDarkTheme
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun ImagePreviewScreen(
    image: ImagePreviewData,
    onDismiss: () -> Unit
) {
    val isDarkTheme = isAppInDarkTheme()
    val backgroundColor = if (isDarkTheme) {
        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.96f)
    } else {
        MaterialTheme.colorScheme.background
    }
    val contentColor = if (isDarkTheme) {
        androidx.compose.ui.graphics.Color.White
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    AppBackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = image.bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(maxWidth)
                    .padding(24.dp)
            )
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = statusBarTopPadding + 6.dp, end = 12.dp)
                .size(DefaultAppBarButtonSize)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(Res.string.action_cancel),
                tint = contentColor
            )
        }
    }
}
