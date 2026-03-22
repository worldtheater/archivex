package com.worldtheater.archive.feature.settings.screens

internal data class DebugMermaidCacheExportResult(
    val exportedFileCount: Int,
    val directoryPath: String
)

internal expect suspend fun exportDebugMermaidCacheToDocuments(): DebugMermaidCacheExportResult
