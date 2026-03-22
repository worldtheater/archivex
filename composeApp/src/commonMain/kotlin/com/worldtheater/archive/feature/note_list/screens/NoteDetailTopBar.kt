package com.worldtheater.archive.feature.note_list.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldtheater.archive.ui.theme.DefaultAppBarButtonSize
import com.worldtheater.archive.ui.theme.appOnSurfaceA30
import com.worldtheater.archive.ui.theme.appOnSurfaceA60
import com.worldtheater.archive.ui.widget.AutoResizingText
import com.worldtheater.archive.ui.widget.CenteredAppTopBar
import com.worldtheater.archive.ui.widget.EditableAppTitle
import com.worldtheater.archive.ui.widget.ScaleToFitBox

data class NoteDetailTopBarStrings(
    val newNotePlaceholder: String,
    val editNotePlaceholder: String,
    val notePreview: String,
    val charsLabel: String,
    val backDescription: String,
    val saveDescription: String,
    val editDescription: String
)

@Composable
fun NoteDetailTopBar(
    isEditing: Boolean,
    isNewNote: Boolean,
    noteTitle: String,
    noteBodyLength: Int,
    previewSubtitle: String?,
    centerContentVisible: Boolean,
    canSave: Boolean,
    strings: NoteDetailTopBarStrings,
    onTitleChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenteredAppTopBar(
        modifier = modifier,
        centerContentVisible = centerContentVisible,
        centerContent = {
            if (isEditing) {
                EditableAppTitle(
                    text = noteTitle,
                    onTextChanged = onTitleChange,
                    placeholder = if (isNewNote) {
                        strings.newNotePlaceholder
                    } else {
                        strings.editNotePlaceholder
                    },
                    placeCursorAtPlaceholderStartWhenEmptyFocused = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                )
            } else {
                if (noteTitle.isEmpty()) {
                    ScaleToFitBox {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$noteBodyLength ${strings.charsLabel}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!previewSubtitle.isNullOrBlank()) {
                                Text(
                                    text = previewSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = appOnSurfaceA60()
                                )
                            }
                        }
                    }
                } else {
                    AutoResizingText(
                        text = strings.notePreview,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(DefaultAppBarButtonSize)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    strings.backDescription
                )
            }
        },
        actions = {
            if (isEditing) {
                IconButton(
                    onClick = onSaveClick,
                    enabled = canSave,
                    modifier = Modifier.size(DefaultAppBarButtonSize)
                ) {
                    Icon(
                        Icons.Filled.Check,
                        strings.saveDescription,
                        modifier = Modifier.size(20.dp),
                        tint = if (canSave) LocalContentColor.current else appOnSurfaceA30()
                    )
                }
            } else {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(DefaultAppBarButtonSize)
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        strings.editDescription,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    )
}
