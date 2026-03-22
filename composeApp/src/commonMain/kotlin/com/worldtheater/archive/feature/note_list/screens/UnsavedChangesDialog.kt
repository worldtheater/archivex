package com.worldtheater.archive.feature.note_list.screens

import androidx.compose.runtime.Composable
import com.worldtheater.archive.ui.widget.AppDialog
import com.worldtheater.archive.ui.widget.IosAlert

data class UnsavedChangesDialogStrings(
    val title: String,
    val description: String,
    val saveText: String,
    val discardText: String
)

@Composable
fun UnsavedChangesDialog(
    strings: UnsavedChangesDialogStrings,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    AppDialog(onDismissRequest = onDismiss) { requestClose ->
        IosAlert(
            title = strings.title,
            description = strings.description,
            primaryText = strings.saveText,
            onPrimaryClick = {
                onSave()
                requestClose {}
            },
            secondaryText = strings.discardText,
            onSecondaryClick = {
                onDiscard()
                requestClose {}
            }
        )
    }
}
