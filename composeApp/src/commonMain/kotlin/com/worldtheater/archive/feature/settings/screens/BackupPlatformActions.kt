package com.worldtheater.archive.feature.settings.screens

data class BackupPlatformActions(
    val dirSubtitle: (backupDir: String?, emptyText: String) -> String,
    val onSelectBackupDir: () -> Unit,
    val requestRestoreDocument: suspend () -> String?
)
