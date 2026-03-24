@file:OptIn(ExperimentalLayoutApi::class)

package com.worldtheater.archive.feature.note_list.screens

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import archivex.composeapp.generated.resources.*
import com.worldtheater.archive.domain.model.MAX_NOTE_BODY_LENGTH
import com.worldtheater.archive.domain.model.NoteInfoSaver
import com.worldtheater.archive.domain.model.emptyNoteInfo
import com.worldtheater.archive.domain.repository.SettingsRepository
import com.worldtheater.archive.platform.system.AppBackHandler
import com.worldtheater.archive.platform.system.DateTimeFormatter
import com.worldtheater.archive.platform.system.UserMessageSink
import com.worldtheater.archive.platform.system.currentTimeMillis
import com.worldtheater.archive.platform.system.logPreviewNoteForDebug
import com.worldtheater.archive.platform.system.shouldSyncPreviewAndEditScroll
import com.worldtheater.archive.ui.theme.COLORS
import com.worldtheater.archive.ui.theme.rememberContentTopPadding
import com.worldtheater.archive.ui.widget.ImagePreviewData
import com.worldtheater.archive.ui.widget.ImagePreviewScreen
import com.worldtheater.archive.util.EditHistoryManager
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private const val MAX_UNDO_STEPS = 30
private const val AUTO_SAVE_INTERVAL_MS = 15_000L
private const val AUTO_SAVE_CHANGE_THRESHOLD = 80

@Composable
fun NoteDetailScreen(
    noteId: Int,
    parentFolderId: Int? = null,
    viewModel: NoteListViewModel,
    onBack: () -> Unit
) {
    val settingsRepository: SettingsRepository = koinInject()
    val dateTimeFormatter: DateTimeFormatter = koinInject()
    val userMessageSink: UserMessageSink = koinInject()
    val isNewNote = noteId == -1
    // Find note from state or use empty.
    // We use a side effect to initialize the draft to avoid resetting on recomposition unless noteId changes
    // But since we want to edit, we need a local state variable for the text.

    var currentNote by rememberSaveable(stateSaver = NoteInfoSaver) {
        mutableStateOf(emptyNoteInfo())
    }
    var initialNote by rememberSaveable(stateSaver = NoteInfoSaver) {
        mutableStateOf(emptyNoteInfo())
    }
    var isInitialized by rememberSaveable { mutableStateOf(false) }
    // Start in edit mode for new notes, view mode for existing
    var isEditing by rememberSaveable { mutableStateOf(isNewNote) }
    var showUnsavedDialog by rememberSaveable { mutableStateOf(false) }
    var sensitiveBodyLoaded by rememberSaveable(noteId) { mutableStateOf(isNewNote) }
    var lastLoggedPreviewSnapshot by rememberSaveable(noteId) { mutableStateOf<String?>(null) }

    val editorAutoSaveEnabled by settingsRepository.editorAutoSaveEnabledFlow.collectAsState()
    val noteBodyTooLongMessage = stringResource(Res.string.msg_note_body_too_long_fmt, MAX_NOTE_BODY_LENGTH)
    val sensitiveNeedsTitleMessage = stringResource(Res.string.msg_sensitive_needs_title)
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var hasRequestedFocus by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Autofocus logic for new notes: trigger only when transition is finished (RESUMED state)
    LaunchedEffect(lifecycleOwner.lifecycle.currentStateAsState().value, isInitialized) {
        if (isInitialized && isNewNote && !hasRequestedFocus &&
            lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED
        ) {
            focusRequester.requestFocus()
            keyboardController?.show()
            hasRequestedFocus = true
        }
    }

    LaunchedEffect(noteId) {
        if (!isInitialized) {
            if (!isNewNote) {
                val found = viewModel.getNoteForViewById(noteId)
                if (found != null) {
                    currentNote = found
                    initialNote = found
                    sensitiveBodyLoaded = !found.isSensitive
                }
            } else {
                val newNote = emptyNoteInfo().copy(
                    color = COLORS.random().toArgb(),
                    parentNodeId = parentFolderId?.let { id ->
                        viewModel.state.value.find { it._id == id }?.nodeId
                    }
                )
                currentNote = newNote
                initialNote = newNote
                sensitiveBodyLoaded = true
            }
            isInitialized = true
        }
    }

    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()

    LaunchedEffect(
        isInitialized,
        isNewNote,
        initialNote.isSensitive,
        sensitiveBodyLoaded,
        noteId
    ) {
        if (!isInitialized || isNewNote) return@LaunchedEffect
        if (!initialNote.isSensitive || sensitiveBodyLoaded) return@LaunchedEffect
        val full = viewModel.getNoteById(noteId)
        if (full != null) {
            currentNote = full
            initialNote = full
        }
        sensitiveBodyLoaded = true
    }

    if (!isInitialized || (!isNewNote && initialNote.isSensitive && !sensitiveBodyLoaded)) {
        NoteDetailLoadingContent(showProgress = isInitialized)
        return
    }

    // Hoist preview state to avoid re-parsing when switching modes without changes
    var previewMarkdown by rememberSaveable { mutableStateOf("") }
    var expandedPreviewImage by remember { mutableStateOf<ImagePreviewData?>(null) }
    // State for markdown toolbar support
    // We use TextFieldValue to manage cursor position for markdown insertion
    var bodyTextFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(currentNote.body))
    }

    val isModified = remember(currentNote, initialNote) {
        currentNote.title != initialNote.title ||
                currentNote.body != initialNote.body ||
                currentNote.isSensitive != initialNote.isSensitive
    }

    // Sync preview + editor buffer with note body when staying in preview mode.
    // This is required for sensitive notes: body may be loaded after auth.
    LaunchedEffect(isEditing, currentNote.body) {
        if (!isEditing) {
            previewMarkdown = currentNote.body
            bodyTextFieldValue = TextFieldValue(currentNote.body)
            // Safety net: Ensure keyboard and focus are cleared whenever we leave edit mode.
            // This handles race conditions where a late touch event might trigger the keyboard
            // *after* the imperative hide() call but *before* the UI reconstruction.
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    LaunchedEffect(
        isInitialized,
        isEditing,
        sensitiveBodyLoaded,
        currentNote.title,
        currentNote.body
    ) {
        if (!isInitialized || isEditing || !sensitiveBodyLoaded) return@LaunchedEffect
        val snapshot = "${currentNote.title}\u0000${currentNote.body}"
        if (lastLoggedPreviewSnapshot == snapshot) return@LaunchedEffect
        logPreviewNoteForDebug(
            title = currentNote.title,
            body = currentNote.body
        )
        lastLoggedPreviewSnapshot = snapshot
    }

    val contentTopPadding = rememberContentTopPadding()
    var previewAtTop by remember { mutableStateOf(true) }

    // Initialize previewMarkdown on first load
    LaunchedEffect(isInitialized) {
        if (isInitialized && previewMarkdown.isEmpty()) {
            previewMarkdown = currentNote.body
        }
    }

    // Optimization for long notes: Defer rendering to avoid blocking navigation transition
    val isLongNote = previewMarkdown.length >= 1000

    // State to control visibility.
    // If short, show immediately. If long, start hidden and show after effect.
    // Hoisted to NoteDetailScreen so that it persists when switching between Edit/Preview modes.
    var showMarkdown by rememberSaveable(previewMarkdown) { mutableStateOf(!isLongNote) }

    // Loading trigger
    if (isLongNote && !showMarkdown) {
        LaunchedEffect(previewMarkdown, lifecycleState) {
            // Wait for the transition to finish (RESUMED state) before rendering heavy Markdown.
            // This replaces the fixed delay logic, ensuring stability for both First Entry (waits for transition)
            // and Mode Switch (renders immediately as already RESUMED).
            if (lifecycleState == Lifecycle.State.RESUMED) {
                showMarkdown = true
            }
        }
    }

    // Separate scroll states to avoid conflict between background Preview and foreground Editor
    val previewScrollState = rememberScrollState()
    val editScrollState = rememberScrollState()

    // Sync scroll positions when switching modes
    LaunchedEffect(isEditing) {
        if (!shouldSyncPreviewAndEditScroll()) return@LaunchedEffect
        if (isEditing) {
            editScrollState.scrollTo(previewScrollState.value)
        } else {
            previewScrollState.scrollTo(editScrollState.value)
        }
    }

    var bodyTooLongNotified by rememberSaveable(noteId) { mutableStateOf(false) }
    val editHistoryManager = remember(noteId) { EditHistoryManager(MAX_UNDO_STEPS) }
    var changedCharsSinceAutoSave by rememberSaveable(noteId) { mutableIntStateOf(0) }

    fun clearSensitiveMemoryState() {
        if (!currentNote.isSensitive && !initialNote.isSensitive) return
        currentNote = currentNote.copy(body = "")
        initialNote = initialNote.copy(body = "")
        previewMarkdown = ""
        bodyTextFieldValue = TextFieldValue("")
        sensitiveBodyLoaded = false
        changedCharsSinceAutoSave = 0
        editHistoryManager.reset()
    }

    val exitDetail = onBack

    val onBackPress = {
        keyboardController?.hide()
        if (isEditing && !isNewNote) {
            focusManager.clearFocus()
            // Sync markdown immediately to prevent flash of old content
            previewMarkdown = currentNote.body
            isEditing = false
        } else {
            if (isModified) {
                showUnsavedDialog = true
            } else {
                exitDetail()
            }
        }
    }

    // Intercept back press handling
    AppBackHandler(enabled = true, onBack = onBackPress)

    DisposableEffect(noteId) {
        onDispose {
            clearSensitiveMemoryState()
        }
    }

    fun applyEditorChange(newValue: TextFieldValue, recordHistory: Boolean = true) {
        if (newValue.text.length > MAX_NOTE_BODY_LENGTH) {
            if (!bodyTooLongNotified) {
                bodyTooLongNotified = true
                userMessageSink.showShort(noteBodyTooLongMessage)
            }
            return
        }
        if (bodyTooLongNotified) {
            bodyTooLongNotified = false
        }
        if (recordHistory && newValue != bodyTextFieldValue) {
            editHistoryManager.recordBeforeChange(bodyTextFieldValue)
        }
        val delta = kotlin.math.abs(newValue.text.length - bodyTextFieldValue.text.length) +
                if (newValue.text != bodyTextFieldValue.text) 1 else 0
        changedCharsSinceAutoSave += delta
        bodyTextFieldValue = newValue
        currentNote = currentNote.copy(body = newValue.text)
    }

    fun saveExistingDraft() {
        if (isNewNote) return
        val now = currentTimeMillis()
        val noteToSave = currentNote.copy(updateTime = now)
        currentNote = noteToSave
        viewModel.updateNote(noteToSave)
        initialNote = noteToSave
        changedCharsSinceAutoSave = 0
    }

    LaunchedEffect(noteId) {
        editHistoryManager.reset()
        changedCharsSinceAutoSave = 0
    }

    LaunchedEffect(
        editorAutoSaveEnabled,
        isEditing,
        isNewNote,
        currentNote.title,
        currentNote.body,
        currentNote.isSensitive,
        initialNote.title,
        initialNote.body,
        initialNote.isSensitive
    ) {
        if (editorAutoSaveEnabled && isEditing && !isNewNote) {
            while (isEditing) {
                delay(AUTO_SAVE_INTERVAL_MS)
                val modified = currentNote.title != initialNote.title ||
                        currentNote.body != initialNote.body ||
                        currentNote.isSensitive != initialNote.isSensitive
                if (modified) {
                    saveExistingDraft()
                }
            }
        }
    }

    LaunchedEffect(
        changedCharsSinceAutoSave,
        editorAutoSaveEnabled,
        isEditing,
        isNewNote,
        currentNote.title,
        currentNote.body,
        currentNote.isSensitive,
        initialNote.title,
        initialNote.body,
        initialNote.isSensitive
    ) {
        val modified = currentNote.title != initialNote.title ||
                currentNote.body != initialNote.body ||
                currentNote.isSensitive != initialNote.isSensitive
        if (editorAutoSaveEnabled && isEditing && !isNewNote && modified &&
            changedCharsSinceAutoSave >= AUTO_SAVE_CHANGE_THRESHOLD
        ) {
            saveExistingDraft()
        }
    }

    NoteDetailScreenContent(
        isEditing = isEditing,
        isNewNote = isNewNote,
        noteTitle = currentNote.title,
        noteBody = currentNote.body,
        noteUpdatedSubtitle = stringResource(
            Res.string.note_updated_at_fmt,
            dateTimeFormatter.formatDateTime(currentNote.updateTime)
        ),
        previewMetadataText = if (currentNote.title.isNotEmpty()) {
            "${currentNote.body.length} ${stringResource(Res.string.chars)}  |  " +
                    stringResource(
                        Res.string.note_updated_at_fmt,
                        dateTimeFormatter.formatDateTime(currentNote.updateTime)
                    )
        } else {
            null
        },
        bodyTextFieldValue = bodyTextFieldValue,
        onBodyChange = { applyEditorChange(it, recordHistory = true) },
        onUndo = {
            editHistoryManager.undo(bodyTextFieldValue)?.let {
                applyEditorChange(it, recordHistory = false)
            }
        },
        onRedo = {
            editHistoryManager.redo(bodyTextFieldValue)?.let {
                applyEditorChange(it, recordHistory = false)
            }
        },
        canUndo = editHistoryManager.canUndo(),
        canRedo = editHistoryManager.canRedo(),
        previewScrollState = previewScrollState,
        editScrollState = editScrollState,
        focusRequester = focusRequester,
        contentTopPadding = contentTopPadding,
        showMarkdown = showMarkdown,
        isLongNote = isLongNote,
        previewMarkdown = previewMarkdown,
        onMermaidImageClick = { previewImage ->
            expandedPreviewImage = previewImage
        },
        previewAtTop = previewAtTop,
        onPreviewAtTopChanged = { previewAtTop = it },
        onTitleChange = { currentNote = currentNote.copy(title = it) },
        onBackClick = { onBackPress() },
        onSaveClick = {
            keyboardController?.hide()
            if (currentNote.isSensitive && currentNote.title.isBlank()) {
                userMessageSink.showShort(sensitiveNeedsTitleMessage)
            } else {
                if (isNewNote) {
                    viewModel.addNote(
                        currentNote,
                        onSuccess = {
                            initialNote = currentNote
                            exitDetail()
                        }
                    )
                } else {
                    saveExistingDraft()
                    initialNote = currentNote
                    focusManager.clearFocus()
                    previewMarkdown = currentNote.body
                    isEditing = false
                }
            }
        },
        onEditClick = { isEditing = true },
        showUnsavedDialog = showUnsavedDialog,
        onUnsavedDismiss = { showUnsavedDialog = false },
        onUnsavedSave = {
            if (currentNote.isSensitive && currentNote.title.isBlank()) {
                userMessageSink.showShort(sensitiveNeedsTitleMessage)
            } else {
                if (isNewNote) {
                    viewModel.addNote(
                        currentNote,
                        onSuccess = { exitDetail() }
                    )
                } else {
                    saveExistingDraft()
                    exitDetail()
                }
            }
        },
        onUnsavedDiscard = { exitDetail() },
        editContentStrings = NoteEditContentStrings(
            placeholder = stringResource(Res.string.write_something),
            undoDescription = stringResource(Res.string.action_undo),
            redoDescription = stringResource(Res.string.action_redo)
        ),
        topBarStrings = NoteDetailTopBarStrings(
            newNotePlaceholder = stringResource(Res.string.title_tap_to_edit),
            editNotePlaceholder = stringResource(Res.string.title_tap_to_edit),
            notePreview = stringResource(Res.string.note_preview),
            charsLabel = stringResource(Res.string.chars),
            backDescription = stringResource(Res.string.back_desc),
            saveDescription = stringResource(Res.string.action_save),
            editDescription = stringResource(Res.string.action_edit)
        ),
        unsavedDialogStrings = UnsavedChangesDialogStrings(
            title = stringResource(Res.string.unsaved_changes_title),
            description = stringResource(Res.string.unsaved_changes_message),
            saveText = stringResource(Res.string.action_save),
            discardText = stringResource(Res.string.action_discard)
        )
    )

    expandedPreviewImage?.let { image ->
        ImagePreviewScreen(
            image = image,
            onDismiss = { expandedPreviewImage = null }
        )
    }
}
