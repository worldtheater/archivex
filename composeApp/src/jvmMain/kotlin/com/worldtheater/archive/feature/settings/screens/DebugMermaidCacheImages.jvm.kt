package com.worldtheater.archive.feature.settings.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.worldtheater.archive.feature.note_list.screens.markdown_preview.MermaidSnapshotCacheIo
import com.worldtheater.archive.feature.note_list.screens.markdown_preview.rememberPlatformMermaidSnapshotCacheIo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

@Composable
internal actual fun rememberDebugMermaidCacheImageLoader(): DebugMermaidCacheImageLoader {
    val cacheIo = rememberPlatformMermaidSnapshotCacheIo()
    return remember(cacheIo) { JvmDebugMermaidCacheImageLoader(cacheIo) }
}

private class JvmDebugMermaidCacheImageLoader(
    private val cacheIo: MermaidSnapshotCacheIo
) : DebugMermaidCacheImageLoader {
    override suspend fun loadRecentImages(limit: Int): List<DebugMermaidCacheImage> = withContext(Dispatchers.IO) {
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

    override suspend fun decodeImage(path: String): DebugMermaidCacheDecodedImage? = withContext(Dispatchers.IO) {
        val bytes = cacheIo.readBytes(path) ?: return@withContext null
        val image = ImageIO.read(ByteArrayInputStream(bytes)) ?: return@withContext null
        DebugMermaidCacheDecodedImage(
            bitmap = image.toComposeImageBitmap(),
            widthPx = image.width,
            heightPx = image.height
        )
    }
}
