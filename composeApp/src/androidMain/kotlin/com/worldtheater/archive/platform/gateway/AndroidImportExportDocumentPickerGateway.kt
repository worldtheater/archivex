package com.worldtheater.archive.platform.gateway

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

class AndroidImportExportDocumentPickerGateway : ImportExportDocumentPickerGateway {
    @Composable
    override fun rememberActions(
        onImportDocumentPicked: (String?) -> Unit,
        onExportDocumentPicked: (String?) -> Unit
    ): ImportExportDocumentPickerActions {
        val plainImportLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                onImportDocumentPicked(uri?.toString())
            }

        val plainExportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            onExportDocumentPicked(uri?.toString())
        }

        return ImportExportDocumentPickerActions(
            launchImportPicker = {
                plainImportLauncher.launch(arrayOf("application/json", "text/plain"))
            },
            launchExportPicker = { fileName ->
                plainExportLauncher.launch(fileName)
            }
        )
    }
}
