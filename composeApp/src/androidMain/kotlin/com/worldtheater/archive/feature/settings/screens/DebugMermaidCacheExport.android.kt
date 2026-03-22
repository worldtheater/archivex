package com.worldtheater.archive.feature.settings.screens

internal actual suspend fun exportDebugMermaidCacheToDocuments(): DebugMermaidCacheExportResult {
    return DebugMermaidCacheExportResult(
        exportedFileCount = 0,
        directoryPath = "unsupported"
    )
}
