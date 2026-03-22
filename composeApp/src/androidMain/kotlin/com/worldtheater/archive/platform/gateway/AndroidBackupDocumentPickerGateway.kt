package com.worldtheater.archive.platform.gateway

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidBackupDocumentPickerGateway : BackupDocumentPickerGateway {
    @Composable
    override fun rememberActions(
        onBackupDirPicked: (String?) -> Unit
    ): BackupDocumentPickerActions {
        var pendingRestoreResult by remember { mutableStateOf<((String?) -> Unit)?>(null) }

        val dirPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            onBackupDirPicked(uri?.toString())
        }

        val openDocumentLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                pendingRestoreResult?.invoke(uri?.toString())
                pendingRestoreResult = null
            }

        return BackupDocumentPickerActions(
            launchBackupDirPicker = { dirPickerLauncher.launch(null) },
            requestRestoreDocument = {
                suspendCancellableCoroutine { continuation ->
                    pendingRestoreResult = { pickedUri ->
                        if (continuation.isActive) {
                            continuation.resume(pickedUri)
                        }
                    }
                    openDocumentLauncher.launch(arrayOf("application/octet-stream"))
                    continuation.invokeOnCancellation { pendingRestoreResult = null }
                }
            }
        )
    }
}
