package com.worldtheater.archive.platform.gateway

import androidx.compose.runtime.Composable

data class BackupDocumentPickerActions(
    val launchBackupDirPicker: () -> Unit,
    val requestRestoreDocument: suspend () -> String?
)

interface BackupDocumentPickerGateway {
    @Composable
    fun rememberActions(
        onBackupDirPicked: (String?) -> Unit
    ): BackupDocumentPickerActions
}

data class ImportExportDocumentPickerActions(
    val launchImportPicker: () -> Unit,
    val launchExportPicker: (String) -> Unit
)

interface ImportExportDocumentPickerGateway {
    @Composable
    fun rememberActions(
        onImportDocumentPicked: (String?) -> Unit,
        onExportDocumentPicked: (String?) -> Unit
    ): ImportExportDocumentPickerActions
}
