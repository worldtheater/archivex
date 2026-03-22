package com.worldtheater.archive.platform.gateway

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

object DesktopBackupDirectoryGateway : BackupDirectoryGateway {
    override fun persistBackupDirectoryPermission(uriString: String) = Unit

    override fun readableBackupDirectoryPath(uriString: String): String {
        return File(uriString).absolutePath
    }
}

class DesktopBackupDocumentPickerGateway : BackupDocumentPickerGateway {
    @Composable
    override fun rememberActions(
        onBackupDirPicked: (String?) -> Unit
    ): BackupDocumentPickerActions {
        val selectDirectory = remember {
            {
                onBackupDirPicked(chooseDirectoryPath())
            }
        }
        val requestRestoreDocument = remember {
            suspend { chooseOpenFilePath() }
        }
        return BackupDocumentPickerActions(
            launchBackupDirPicker = selectDirectory,
            requestRestoreDocument = requestRestoreDocument
        )
    }
}

class DesktopImportExportDocumentPickerGateway : ImportExportDocumentPickerGateway {
    @Composable
    override fun rememberActions(
        onImportDocumentPicked: (String?) -> Unit,
        onExportDocumentPicked: (String?) -> Unit
    ): ImportExportDocumentPickerActions {
        val importPicker = remember {
            { onImportDocumentPicked(chooseOpenFilePath()) }
        }
        val exportPicker = remember {
            { defaultName: String -> onExportDocumentPicked(chooseSaveFilePath(defaultName)) }
        }
        return ImportExportDocumentPickerActions(
            launchImportPicker = importPicker,
            launchExportPicker = exportPicker
        )
    }
}

private fun chooseDirectoryPath(): String? {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isMultiSelectionEnabled = false
    }
    return showChooser(chooser, saveDialog = false)
}

private fun chooseOpenFilePath(): String? {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.FILES_ONLY
        isMultiSelectionEnabled = false
    }
    return showChooser(chooser, saveDialog = false)
}

private fun chooseSaveFilePath(defaultName: String): String? {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.FILES_ONLY
        selectedFile = File(defaultName)
        isMultiSelectionEnabled = false
    }
    return showChooser(chooser, saveDialog = true)
}

private fun showChooser(chooser: JFileChooser, saveDialog: Boolean): String? {
    var selectedPath: String? = null
    runOnEdtAndWait {
        val result = if (saveDialog) {
            chooser.showSaveDialog(null)
        } else {
            chooser.showOpenDialog(null)
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedPath = chooser.selectedFile?.absolutePath
        }
    }
    return selectedPath
}

private fun runOnEdtAndWait(action: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) {
        action()
    } else {
        SwingUtilities.invokeAndWait(action)
    }
}
