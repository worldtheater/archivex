package com.worldtheater.archive.feature.note_list.screens.markdown_preview

import androidx.compose.runtime.Composable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class MermaidSnapshotCachedDiagramPayload(
    val pngBytes: ByteArray,
    val widthDp: Int,
    val heightDp: Int,
)

internal data class MermaidSnapshotCacheFileEntry(
    val name: String,
    val lastModifiedEpochMillis: Long,
)

internal interface MermaidSnapshotCacheIo {
    suspend fun ensureCacheDirectory(): String
    suspend fun readBytes(path: String): ByteArray?
    suspend fun writeBytes(path: String, bytes: ByteArray)
    suspend fun readText(path: String): String?
    suspend fun writeText(path: String, text: String)
    suspend fun listFiles(directoryPath: String): List<MermaidSnapshotCacheFileEntry>
    suspend fun deleteFile(path: String)
}

@Composable
internal expect fun rememberPlatformMermaidSnapshotCacheIo(): MermaidSnapshotCacheIo

internal class MermaidSnapshotDiagramCache(
    private val io: MermaidSnapshotCacheIo,
    private val maxMemoryEntries: Int = 24,
    private val maxDiskEntries: Int = 64,
) {
    private val memoryCache = linkedMapOf<MermaidSnapshotCacheKey, MermaidSnapshotCachedDiagramPayload>()
    private val memoryCacheMutex = Mutex()

    suspend fun get(key: MermaidSnapshotCacheKey): MermaidSnapshotCachedDiagramPayload? {
        getFromMemory(key)?.let { return it }
        val diskCached = loadFromDisk(key) ?: return null
        putInMemory(key, diskCached)
        return diskCached
    }

    suspend fun put(key: MermaidSnapshotCacheKey, value: MermaidSnapshotCachedDiagramPayload) {
        putInMemory(key, value)
        saveToDisk(key, value)
    }

    private suspend fun getFromMemory(key: MermaidSnapshotCacheKey): MermaidSnapshotCachedDiagramPayload? {
        return memoryCacheMutex.withLock {
            val cached = memoryCache.remove(key) ?: return@withLock null
            memoryCache[key] = cached
            cached
        }
    }

    private suspend fun putInMemory(key: MermaidSnapshotCacheKey, value: MermaidSnapshotCachedDiagramPayload) {
        memoryCacheMutex.withLock {
            memoryCache.remove(key)
            memoryCache[key] = value
            while (memoryCache.size > maxMemoryEntries) {
                val eldestKey = memoryCache.entries.firstOrNull()?.key ?: break
                memoryCache.remove(eldestKey)
            }
        }
    }

    private suspend fun loadFromDisk(key: MermaidSnapshotCacheKey): MermaidSnapshotCachedDiagramPayload? {
        val cacheDir = io.ensureCacheDirectory()
        val fingerprint = cacheFingerprint(key)
        val pngBytes = io.readBytes("$cacheDir/$fingerprint.png") ?: return null
        val metadata = io.readText("$cacheDir/$fingerprint.meta") ?: return null
        val dimensions = parseMetadata(metadata) ?: return null
        return MermaidSnapshotCachedDiagramPayload(
            pngBytes = pngBytes,
            widthDp = dimensions.first,
            heightDp = dimensions.second
        )
    }

    private suspend fun saveToDisk(key: MermaidSnapshotCacheKey, value: MermaidSnapshotCachedDiagramPayload) {
        runCatching {
            val cacheDir = io.ensureCacheDirectory()
            val fingerprint = cacheFingerprint(key)
            io.writeBytes("$cacheDir/$fingerprint.png", value.pngBytes)
            io.writeText("$cacheDir/$fingerprint.meta", buildMetadata(value.widthDp, value.heightDp))
            trimDiskIfNeeded(cacheDir)
        }
    }

    private suspend fun trimDiskIfNeeded(cacheDir: String) {
        val pngFiles = io.listFiles(cacheDir)
            .filter { it.name.endsWith(".png") }
            .sortedBy { it.lastModifiedEpochMillis }
        val overflow = pngFiles.size - maxDiskEntries
        if (overflow <= 0) return
        pngFiles.take(overflow).forEach { imageEntry ->
            runCatching {
                io.deleteFile("$cacheDir/${imageEntry.name}")
                io.deleteFile("$cacheDir/${imageEntry.name.removeSuffix(".png")}.meta")
            }
        }
    }

    private fun buildMetadata(widthDp: Int, heightDp: Int): String {
        return "widthDp=$widthDp\nheightDp=$heightDp\n"
    }

    private fun parseMetadata(metadata: String): Pair<Int, Int>? {
        var widthDp: Int? = null
        var heightDp: Int? = null
        metadata.lineSequence().forEach { line ->
            val separatorIndex = line.indexOf('=')
            if (separatorIndex <= 0) return@forEach
            val key = line.substring(0, separatorIndex)
            val value = line.substring(separatorIndex + 1)
            when (key) {
                "widthDp" -> widthDp = value.toIntOrNull()
                "heightDp" -> heightDp = value.toIntOrNull()
            }
        }
        val safeWidthDp = widthDp ?: return null
        val safeHeightDp = heightDp ?: return null
        return safeWidthDp to safeHeightDp
    }
}

internal fun MermaidSnapshotDiagram.toCachedPayload(): MermaidSnapshotCachedDiagramPayload {
    return MermaidSnapshotCachedDiagramPayload(
        pngBytes = pngBytes,
        widthDp = widthDp,
        heightDp = heightDp
    )
}

internal suspend fun clearMermaidSnapshotDiskCache(io: MermaidSnapshotCacheIo): Int {
    val cacheDir = io.ensureCacheDirectory()
    val files = io.listFiles(cacheDir)
    files.forEach { entry ->
        io.deleteFile("$cacheDir/${entry.name}")
    }
    return files.size
}

private fun cacheFingerprint(key: MermaidSnapshotCacheKey): String {
    val input = buildString {
        append(key.cacheVersion)
        append('\n')
        append(key.renderWidthPx)
        append('\n')
        append(key.mermaidCode)
        append('\n')
        append(key.themeConfig)
    }
    var hash = 14695981039346656037uL
    input.encodeToByteArray().forEach { byte ->
        hash = (hash xor byte.toUByte().toULong()) * 1099511628211uL
    }
    return hash.toString(16)
}
