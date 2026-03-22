@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.worldtheater.archive.feature.settings.screens

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.posix.mkdir

private const val IOS_EXPORT_DIRECTORY_MODE: UShort = 0x1EDu

internal actual suspend fun exportDebugMermaidCacheToDocuments(): DebugMermaidCacheExportResult =
    withContext(Dispatchers.Default) {
        val fileManager = NSFileManager.defaultManager
        val sourceDir = "${NSHomeDirectory()}/Library/Caches/mermaid-preview"
        val destinationRoot = "${NSHomeDirectory()}/Documents"
        val destinationDir = "$destinationRoot/mermaid-export"
        ensureDirectory(destinationRoot)

        if (fileManager.fileExistsAtPath(destinationDir)) {
            fileManager.removeItemAtPath(destinationDir, error = null)
        }
        ensureDirectory(destinationDir)

        val exportedCount = if (fileManager.fileExistsAtPath(sourceDir)) {
            fileManager.contentsOfDirectoryAtPath(sourceDir, error = null)
                ?.mapNotNull { it as? String }
                .orEmpty()
                .count { name ->
                    fileManager.copyItemAtPath(
                        srcPath = "$sourceDir/$name",
                        toPath = "$destinationDir/$name",
                        error = null
                    )
                }
        } else {
            0
        }

        DebugMermaidCacheExportResult(
            exportedFileCount = exportedCount,
            directoryPath = destinationDir
        )
    }

private fun ensureDirectory(path: String) {
    if (NSFileManager.defaultManager.fileExistsAtPath(path)) return
    mkdir(path, IOS_EXPORT_DIRECTORY_MODE)
}
