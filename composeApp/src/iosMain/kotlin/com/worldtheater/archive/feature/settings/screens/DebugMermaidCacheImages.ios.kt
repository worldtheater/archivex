package com.worldtheater.archive.feature.settings.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.worldtheater.archive.feature.note_list.screens.markdown_preview.MermaidSnapshotCacheIo
import com.worldtheater.archive.feature.note_list.screens.markdown_preview.rememberPlatformMermaidSnapshotCacheIo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage

@Composable
internal actual fun rememberDebugMermaidCacheImageLoader(): DebugMermaidCacheImageLoader {
    val cacheIo = rememberPlatformMermaidSnapshotCacheIo()
    return remember(cacheIo) { IosDebugMermaidCacheImageLoader(cacheIo) }
}

private class IosDebugMermaidCacheImageLoader(
    private val cacheIo: MermaidSnapshotCacheIo
) : DebugMermaidCacheImageLoader {
    override suspend fun loadRecentImages(limit: Int): List<DebugMermaidCacheImage> = withContext(Dispatchers.Default) {
        val cacheDir = cacheIo.ensureCacheDirectory()
        cacheIo.listFiles(cacheDir)
            .filter { it.name.endsWith(".png") }
            .sortedByDescending { it.lastModifiedEpochMillis }
            .take(limit)
            .map { entry ->
                DebugMermaidCacheImage(
                    id = entry.name,
                    path = "$cacheDir/${entry.name}"
                )
            }
    }

    override suspend fun decodeImage(path: String): DebugMermaidCacheDecodedImage? = withContext(Dispatchers.Default) {
        val bytes = cacheIo.readBytes(path) ?: return@withContext null
        val skiaImage = runCatching { SkiaImage.makeFromEncoded(bytes) }.getOrNull() ?: return@withContext null
        DebugMermaidCacheDecodedImage(
            bitmap = skiaImage.toComposeImageBitmap(),
            widthPx = skiaImage.width,
            heightPx = skiaImage.height
        )
    }
}
