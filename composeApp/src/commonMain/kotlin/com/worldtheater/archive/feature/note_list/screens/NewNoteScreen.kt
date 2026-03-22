package com.worldtheater.archive.feature.note_list.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import archivex.composeapp.generated.resources.Res
import archivex.composeapp.generated.resources.action_redo
import archivex.composeapp.generated.resources.action_save
import archivex.composeapp.generated.resources.action_undo
import archivex.composeapp.generated.resources.back_desc
import archivex.composeapp.generated.resources.msg_note_body_too_long_fmt
import archivex.composeapp.generated.resources.title_tap_to_edit
import archivex.composeapp.generated.resources.write_something
import com.worldtheater.archive.domain.model.MAX_NOTE_BODY_LENGTH
import com.worldtheater.archive.platform.system.UserMessageSink
import com.worldtheater.archive.ui.theme.DefaultAppBarButtonSize
import com.worldtheater.archive.ui.theme.appOnSurfaceA30
import com.worldtheater.archive.ui.theme.appOnSurfaceA60
import com.worldtheater.archive.ui.theme.rememberContentTopPadding
import com.worldtheater.archive.ui.widget.CenteredAppTopBar
import com.worldtheater.archive.ui.widget.EditableAppTitle
import com.worldtheater.archive.ui.widget.MarkdownShortcutBar
import com.worldtheater.archive.ui.widget.MarkdownUndoRedoLeadingActions
import com.worldtheater.archive.ui.widget.scrollbar
import com.worldtheater.archive.util.EditHistoryManager
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private const val MAX_NEW_NOTE_UNDO_STEPS = 30

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun NewNoteScreen(
    viewModel: NoteListViewModel,
    parentFolderId: Int? = null,
    onBack: () -> Unit
) {
    val userMessageSink: UserMessageSink = koinInject()
    var title by rememberSaveable { mutableStateOf("") }
    var bodyTextFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var bodyTooLongNotified by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val focusRequester = remember { FocusRequester() }
    var hasRequestedFocus by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    val editHistoryManager = remember { EditHistoryManager(MAX_NEW_NOTE_UNDO_STEPS) }

    val writeSomething = stringResource(Res.string.write_something)
    val titleTapToEdit = stringResource(Res.string.title_tap_to_edit)
    val backDesc = stringResource(Res.string.back_desc)
    val actionSave = stringResource(Res.string.action_save)
    val undoDescription = stringResource(Res.string.action_undo)
    val redoDescription = stringResource(Res.string.action_redo)
    val noteBodyTooLongMessage = stringResource(
        Res.string.msg_note_body_too_long_fmt,
        MAX_NOTE_BODY_LENGTH
    )

    fun applyBodyChange(newValue: TextFieldValue, recordHistory: Boolean = true) {
        if (newValue.text.length <= MAX_NOTE_BODY_LENGTH) {
            if (recordHistory && newValue != bodyTextFieldValue) {
                editHistoryManager.recordBeforeChange(bodyTextFieldValue)
            }
            bodyTextFieldValue = newValue
            if (bodyTooLongNotified) {
                bodyTooLongNotified = false
            }
        } else if (!bodyTooLongNotified) {
            bodyTooLongNotified = true
            userMessageSink.showShort(noteBodyTooLongMessage)
        }
    }

    LaunchedEffect(lifecycleState) {
        if (!hasRequestedFocus && lifecycleState == Lifecycle.State.RESUMED) {
            focusRequester.requestFocus()
            keyboardController?.show()
            hasRequestedFocus = true
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        content = { _ ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                            )
                        }
                        .scrollbar(
                            state = scrollState,
                            horizontal = false,
                            alignEnd = true,
                            knobColor = appOnSurfaceA60(),
                            trackColor = Color.Transparent,
                            padding = 2.dp
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        Spacer(Modifier.height(rememberContentTopPadding() + 16.dp))

                        BasicTextField(
                            value = bodyTextFieldValue,
                            onValueChange = { applyBodyChange(it, recordHistory = true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (bodyTextFieldValue.text.isEmpty()) {
                                        Text(
                                            text = writeSomething,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 16.sp,
                                                lineHeight = 24.sp,
                                                color = Color.Gray
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        Spacer(Modifier.height(56.dp))
                        Spacer(Modifier.height(32.dp))
                    }
                }

                AnimatedVisibility(
                    visible = imeVisible,
                    enter = fadeIn(animationSpec = tween(100)),
                    exit = fadeOut(animationSpec = tween(60)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .imePadding()
                ) {
                    MarkdownShortcutBar(
                        value = bodyTextFieldValue,
                        onValueChange = { updated -> applyBodyChange(updated, recordHistory = true) },
                        leadingActions = {
                            MarkdownUndoRedoLeadingActions(
                                canUndo = editHistoryManager.canUndo(),
                                canRedo = editHistoryManager.canRedo(),
                                undoDescription = undoDescription,
                                redoDescription = redoDescription,
                                onUndo = {
                                    editHistoryManager.undo(bodyTextFieldValue)?.let {
                                        applyBodyChange(it, recordHistory = false)
                                    }
                                },
                                onRedo = {
                                    editHistoryManager.redo(bodyTextFieldValue)?.let {
                                        applyBodyChange(it, recordHistory = false)
                                    }
                                }
                            )
                        }
                    )
                }

                CenteredAppTopBar(
                    modifier = Modifier.align(Alignment.TopCenter),
                    centerContentVisible = scrollState.value == 0,
                    centerContent = {
                        EditableAppTitle(
                            text = title,
                            onTextChanged = { title = it },
                            placeholder = titleTapToEdit,
                            placeCursorAtPlaceholderStartWhenEmptyFocused = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp)
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                keyboardController?.hide()
                                onBack()
                            },
                            modifier = Modifier.size(DefaultAppBarButtonSize)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                backDesc
                            )
                        }
                    },
                    actions = {
                        val isSaveEnabled = bodyTextFieldValue.text.isNotBlank()
                        IconButton(
                            onClick = {
                                keyboardController?.hide()
                                if (bodyTextFieldValue.text.isNotEmpty()) {
                                    viewModel.addNote(
                                        title = title,
                                        content = bodyTextFieldValue.text,
                                        parentId = parentFolderId,
                                        onSuccess = onBack
                                    )
                                } else {
                                    onBack()
                                }
                            },
                            enabled = isSaveEnabled,
                            modifier = Modifier.size(DefaultAppBarButtonSize)
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                actionSave,
                                modifier = Modifier.size(20.dp),
                                tint = if (isSaveEnabled) LocalContentColor.current else appOnSurfaceA30()
                            )
                        }
                    }
                )
            }
        }
    )
}
