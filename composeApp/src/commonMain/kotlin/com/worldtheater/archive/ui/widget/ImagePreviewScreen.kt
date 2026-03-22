package com.worldtheater.archive.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

data class ImagePreviewData(
    val bitmap: ImageBitmap,
    val widthDp: Int,
    val heightDp: Int
)

@Composable
expect fun ImagePreviewScreen(
    image: ImagePreviewData,
    onDismiss: () -> Unit
)
