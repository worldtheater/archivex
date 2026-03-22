package com.worldtheater.archive.feature.note_list.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import com.worldtheater.archive.platform.system.shouldDisposePreviewLayerWhileEditing
import com.worldtheater.archive.platform.system.shouldSyncPreviewAndEditScroll

@Composable
fun NoteDetailScreenContent(
    isEditing: Boolean,
    isNewNote: Boolean,
    noteTitle: String,
    noteBody: String,
    noteUpdatedSubtitle: String,
    previewMetadataText: String?,
    bodyTextFieldValue: TextFieldValue,
    onBodyChange: (TextFieldValue) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    previewScrollState: ScrollState,
    editScrollState: ScrollState,
    focusRequester: FocusRequester,
    contentTopPadding: Dp,
    showMarkdown: Boolean,
    isLongNote: Boolean,
    previewMarkdown: String,
    onMermaidImageClick: ((ImageBitmap, Int, Int) -> Unit)?,
    previewAtTop: Boolean,
    onPreviewAtTopChanged: ((Boolean) -> Unit)?,
    onTitleChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onEditClick: () -> Unit,
    showUnsavedDialog: Boolean,
    onUnsavedDismiss: () -> Unit,
    onUnsavedSave: () -> Unit,
    onUnsavedDiscard: () -> Unit,
    editContentStrings: NoteEditContentStrings,
    topBarStrings: NoteDetailTopBarStrings,
    unsavedDialogStrings: UnsavedChangesDialogStrings
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val currentScrollState = if (isEditing) editScrollState else previewScrollState
        val centerContentVisible by remember(currentScrollState, isEditing, previewAtTop) {
            derivedStateOf {
                if (!isEditing && !shouldSyncPreviewAndEditScroll()) {
                    previewAtTop
                } else {
                    currentScrollState.value == 0
                }
            }
        }
        val shouldComposePreview = !isEditing || !shouldDisposePreviewLayerWhileEditing()

        Box(modifier = Modifier.fillMaxSize()) {
            if (shouldComposePreview) {
                NotePreviewContent(
                    title = noteTitle,
                    metadataText = previewMetadataText,
                    scrollState = previewScrollState,
                    contentTopPadding = contentTopPadding,
                    showMarkdown = showMarkdown,
                    isLongNote = isLongNote,
                    markdown = previewMarkdown,
                    onMermaidImageClick = onMermaidImageClick,
                    onAtTopChanged = onPreviewAtTopChanged,
                    modifier = Modifier.alpha(if (isEditing) 0f else 1f)
                )
            }

            if (isEditing) {
                NoteEditContent(
                    bodyText = noteBody,
                    bodyTextFieldValue = bodyTextFieldValue,
                    onBodyChange = onBodyChange,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    scrollState = editScrollState,
                    focusRequester = focusRequester,
                    strings = editContentStrings,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentTopPadding = contentTopPadding
                )
            }
        }

        NoteDetailTopBar(
            isEditing = isEditing,
            isNewNote = isNewNote,
            noteTitle = noteTitle,
            noteBodyLength = noteBody.length,
            previewSubtitle = noteUpdatedSubtitle,
            centerContentVisible = centerContentVisible,
            canSave = noteBody.isNotBlank(),
            strings = topBarStrings,
            onTitleChange = onTitleChange,
            onBackClick = onBackClick,
            onSaveClick = onSaveClick,
            onEditClick = onEditClick,
            modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter)
        )

        if (showUnsavedDialog) {
            UnsavedChangesDialog(
                strings = unsavedDialogStrings,
                onDismiss = onUnsavedDismiss,
                onSave = onUnsavedSave,
                onDiscard = onUnsavedDiscard
            )
        }
    }
}
