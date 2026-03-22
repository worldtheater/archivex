package com.worldtheater.archive.feature.settings.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import archivex.composeapp.generated.resources.*
import com.worldtheater.archive.feature.note_list.screens.NoteListViewModel
import com.worldtheater.archive.platform.gateway.ImportExportDocumentPickerGateway
import com.worldtheater.archive.platform.system.currentTimeMillis
import com.worldtheater.archive.ui.theme.rememberContentTopPadding
import com.worldtheater.archive.ui.widget.SettingsAppTopBar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun ImportExportSettingsScreen(
    viewModel: NoteListViewModel,
    onBack: () -> Unit
) {
    val title = stringResource(Res.string.pref_import_export_title)
    val platformActions = rememberImportExportPlatformActions(
        viewModel = viewModel,
        importExportDocumentPickerGateway = koinInject()
    )
    val transferProgress by viewModel.transferProgress.collectAsState()
    val listState = rememberLazyListState()
    val contentTopPadding = rememberContentTopPadding()
    val transferBusy = transferProgress != null
    val transferProgressText = transferProgress?.let { progress ->
        val title = when (progress.type) {
            NoteListViewModel.TransferType.BACKUP -> stringResource(Res.string.transfer_backup_running)
            NoteListViewModel.TransferType.RESTORE -> stringResource(Res.string.transfer_restore_running)
            NoteListViewModel.TransferType.EXPORT_PLAIN -> stringResource(Res.string.transfer_export_running)
            NoteListViewModel.TransferType.IMPORT_PLAIN -> stringResource(Res.string.transfer_import_running)
        }
        if (progress.totalSteps > 0) {
            stringResource(Res.string.msg_transfer_progress_fmt, title, progress.completedSteps, progress.totalSteps)
        } else {
            title
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ImportExportSettingsContent(
            listState = listState,
            contentTopPadding = contentTopPadding,
            transferProgressText = transferProgressText,
            transferBusy = transferBusy,
            onImportPlainClick = platformActions.onImportPlainClick,
            onExportPlainClick = platformActions.onExportPlainClick
        )

        SettingsAppTopBar(
            title = title,
            onBack = onBack,
            listState = listState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun rememberImportExportPlatformActions(
    viewModel: NoteListViewModel,
    importExportDocumentPickerGateway: ImportExportDocumentPickerGateway
): ImportExportPlatformActions {
    val pickerActions = importExportDocumentPickerGateway.rememberActions(
        onImportDocumentPicked = { uri ->
            if (uri != null) viewModel.importPlainAsNew(uri)
        },
        onExportDocumentPicked = { uri ->
            if (uri != null) viewModel.exportPlain(uri)
        }
    )
    return ImportExportPlatformActions(
        onImportPlainClick = pickerActions.launchImportPicker,
        onExportPlainClick = {
            val fileName = "archive_plain_${currentTimeMillis()}.json"
            pickerActions.launchExportPicker(fileName)
        }
    )
}
