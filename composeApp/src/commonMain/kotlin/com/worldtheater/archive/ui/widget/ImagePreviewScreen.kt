package com.worldtheater.archive.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

data class ImagePreviewSvgExportData(
    val svgText: String,
    val suggestedFileName: String
)

data class ImagePreviewData(
    val bitmap: ImageBitmap,
    val widthDp: Int,
    val heightDp: Int,
    val svgExport: ImagePreviewSvgExportData? = null
)

@Composable
expect fun ImagePreviewScreen(
    image: ImagePreviewData,
    onDismiss: () -> Unit
)
