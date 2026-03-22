package com.worldtheater.archive.feature.settings.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

internal data class DebugMermaidCacheImage(
    val id: String,
    val path: String,
)

internal data class DebugMermaidCacheDecodedImage(
    val bitmap: ImageBitmap,
    val widthPx: Int,
    val heightPx: Int,
)

internal interface DebugMermaidCacheImageLoader {
    suspend fun loadRecentImages(limit: Int): List<DebugMermaidCacheImage>
    suspend fun decodeImage(path: String): DebugMermaidCacheDecodedImage?
}

@Composable
internal expect fun rememberDebugMermaidCacheImageLoader(): DebugMermaidCacheImageLoader
