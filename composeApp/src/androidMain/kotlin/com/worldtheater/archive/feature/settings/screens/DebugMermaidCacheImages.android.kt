package com.worldtheater.archive.feature.settings.screens

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import com.worldtheater.archive.feature.note_list.screens.markdown_preview.MermaidSnapshotCacheIo
import com.worldtheater.archive.feature.note_list.screens.markdown_preview.rememberPlatformMermaidSnapshotCacheIo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal actual fun rememberDebugMermaidCacheImageLoader(): DebugMermaidCacheImageLoader {
    val cacheIo = rememberPlatformMermaidSnapshotCacheIo()
    return remember(cacheIo) { AndroidDebugMermaidCacheImageLoader(cacheIo) }
}

private class AndroidDebugMermaidCacheImageLoader(
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
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
        DebugMermaidCacheDecodedImage(
            bitmap = bitmap.asImageBitmap(),
            widthPx = bitmap.width,
            heightPx = bitmap.height
        )
    }
}
