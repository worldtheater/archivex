package com.worldtheater.archive.feature.note_list.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import archivex.composeapp.generated.resources.*
import com.worldtheater.archive.domain.SensitiveNoteAuthFrequency
import com.worldtheater.archive.domain.model.ITEM_TYPE_FOLDER
import com.worldtheater.archive.domain.model.MAX_FOLDER_DIRECT_ITEMS
import com.worldtheater.archive.domain.model.MAX_FOLDER_NAME_LENGTH
import com.worldtheater.archive.domain.model.MAX_TOTAL_ITEMS
import com.worldtheater.archive.domain.model.NoteInfo
import com.worldtheater.archive.platform.auth.SensitiveAuthAvailability
import com.worldtheater.archive.platform.auth.SensitiveAuthAvailabilityProvider
import com.worldtheater.archive.platform.auth.SensitiveAuthPrompt
import com.worldtheater.archive.platform.auth.SensitiveAuthSessionStore
import com.worldtheater.archive.platform.gateway.ClipboardGateway
import com.worldtheater.archive.platform.gateway.HapticFeedbackGateway
import com.worldtheater.archive.domain.repository.SettingsRepository
import com.worldtheater.archive.platform.system.AppBackHandler
import com.worldtheater.archive.platform.system.DateTimeFormatter
import com.worldtheater.archive.platform.system.UserMessageSink
import com.worldtheater.archive.platform.system.elapsedRealtimeMillis
import com.worldtheater.archive.security.PasswordStrengthLevel
import com.worldtheater.archive.ui.widget.MyPullToRefreshBox
import com.worldtheater.archive.ui.widget.NOTE_TITLE_MAX_LENGTH
import com.worldtheater.archive.util.log.L
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

data class NoteListPlatformActions(
    val vibrate: (milliseconds: Long, effect: Int?) -> Unit,
    val showMessage: (String) -> Unit,
    val clearClipboard: () -> Unit,
    val copyToClipboard: (label: String, text: String) -> Unit,
    val isSensitiveAuthAvailable: () -> Boolean,
    val shouldBlockSensitiveAuth: () -> Boolean,
    val authenticateSensitiveToggle: suspend (title: String, subtitle: String) -> Boolean
)

private sealed interface NoteListDialogState {
    data object None : NoteListDialogState
    data object Create : NoteListDialogState
    data object CreateFolder : NoteListDialogState
    data class Delete(val note: NoteInfo) : NoteListDialogState
    data class RequireTitleForLock(val note: NoteInfo) : NoteListDialogState
    data class ItemDetails(val item: NoteInfo) : NoteListDialogState
}

private const val TAG = "NoteListScreen"
private const val BACK_TO_TOP_MIN_ITEMS = 30

@Composable
fun NoteListScreen(
    viewModel: NoteListViewModel,
    modifier: Modifier = Modifier,
    onNoteClick: (Int) -> Unit,
    onAddNoteClick: (Int?) -> Unit,
    onSettingsClick: () -> Unit
) {
    val platform = rememberNoteListPlatformActions()
    val settingsRepository: SettingsRepository = koinInject()
    val sensitiveAuthSessionStore: SensitiveAuthSessionStore = koinInject()
    val dateTimeFormatter: DateTimeFormatter = koinInject()
    val lockPromptTitle = stringResource(Res.string.biometric_prompt_sensitive_lock_title)
    val lockPromptSubtitle = stringResource(Res.string.biometric_prompt_sensitive_lock_subtitle)
    val unlockPromptTitle = stringResource(Res.string.biometric_prompt_sensitive_unlock_title)
    val unlockPromptSubtitle = stringResource(Res.string.biometric_prompt_sensitive_unlock_subtitle)
    val openPromptTitle = stringResource(Res.string.biometric_prompt_sensitive_open_title)
    val openPromptSubtitle = stringResource(Res.string.biometric_prompt_sensitive_open_subtitle)
    val folderItemsLimitMessage = stringResource(Res.string.msg_folder_items_limit_fmt, MAX_FOLDER_DIRECT_ITEMS)
    val totalItemsLimitMessage = stringResource(Res.string.msg_total_items_limit_fmt, MAX_TOTAL_ITEMS)
    val folderDepthLimitMessage = stringResource(Res.string.msg_folder_depth_limit)
    val moveFailedMessage = stringResource(Res.string.msg_move_failed)
    val clipboardClearedMessage = stringResource(Res.string.msg_clipboard_cleared)
    val passwordCopiedMessage = stringResource(Res.string.msg_password_copied)
    val addNoteDescription = stringResource(Res.string.add_note_desc)
    val confirmMoveDescription = stringResource(Res.string.action_confirm_move)
    val backToTopDescription = stringResource(Res.string.action_back_to_top)
    val moveModeTitle = stringResource(Res.string.move_mode_title)
    val moveModeHint = stringResource(Res.string.move_mode_hint)
    val totalNotesFormatter: @Composable (Int) -> String = { count -> stringResource(Res.string.notes_count_fmt, count) }
    val newNoteDialogTitle = stringResource(Res.string.new_note)
    val actionOk = stringResource(Res.string.action_ok)
    val actionCancel = stringResource(Res.string.action_cancel)
    val writeSomething = stringResource(Res.string.write_something)
    val detailsTitle = stringResource(Res.string.menu_details)
    val strongPasswordTitle = stringResource(Res.string.dialog_strong_password_generator_title)
    val passwordLabel = stringResource(Res.string.password_label)
    val copyPasswordDescription = stringResource(Res.string.action_copy_password)
    val regeneratePasswordText = stringResource(Res.string.action_regenerate_password)
    val passwordLengthFormatter: @Composable (Int) -> String = { length -> stringResource(Res.string.password_length_fmt, length) }
    val passwordStrengthFormatter: @Composable (PasswordStrengthLevel) -> String = { level ->
        val levelRes = when (level) {
            PasswordStrengthLevel.VERY_WEAK -> Res.string.password_strength_very_weak
            PasswordStrengthLevel.WEAK -> Res.string.password_strength_weak
            PasswordStrengthLevel.FAIR -> Res.string.password_strength_fair
            PasswordStrengthLevel.GOOD -> Res.string.password_strength_good
            PasswordStrengthLevel.STRONG -> Res.string.password_strength_strong
        }
        stringResource(Res.string.password_strength_fmt, stringResource(levelRes))
    }

    val noteList by viewModel.state
    val totalNotesCount by viewModel.totalNotesCount
    val scope = rememberCoroutineScope()
    val isSearchActive by viewModel.isSearchActive
    val searchQuery by viewModel.searchQuery
    var currentFolderId by rememberSaveable { mutableStateOf<Int?>(null) }
    val folderListStates = remember { mutableStateMapOf<Int?, LazyListState>() }
    val currentNormalListState = remember(currentFolderId) {
        folderListStates.getOrPut(currentFolderId) {
            val saved = viewModel.getFolderScrollPosition(currentFolderId)
            LazyListState(saved.index, saved.offset)
        }
    }
    val searchListState = remember(searchQuery) { LazyListState() }
    val activeListState = if (isSearchActive) searchListState else currentNormalListState
    val latestActiveListState by rememberUpdatedState(activeListState)
    var lastNavigateAt by remember { mutableLongStateOf(0L) }
    var lastLongPressAt by remember { mutableLongStateOf(0L) }
    var openContextMenuNoteId by remember { mutableStateOf<Int?>(null) }

    var dialogState by remember { mutableStateOf<NoteListDialogState>(NoteListDialogState.None) }
    var showStrongPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var folderNavDirection by remember { mutableStateOf(1) }
    var movingItemId by remember { mutableStateOf<Int?>(null) }
    val isMoveMode = movingItemId != null

    val contentTopPadding = com.worldtheater.archive.ui.theme.rememberContentTopPadding()
    val noteById = remember(noteList) { viewModel.buildNoteById(noteList) }
    val parentIdByNodeId = remember(noteList) { viewModel.buildParentIdByNodeId(noteList) }
    val sortedChildrenByParent =
        remember(noteList) { viewModel.buildSortedChildrenByParent(noteList) }

    AppBackHandler(enabled = isSearchActive) {
        viewModel.setSearchActive(false)
    }

    AppBackHandler(enabled = !isSearchActive && isMoveMode) {
        if (currentFolderId != null) {
            folderNavDirection = -1
            currentFolderId = noteById[currentFolderId]
                ?.parentNodeId
                ?.let { parentIdByNodeId[it] }
        } else {
            movingItemId = null
        }
    }

    AppBackHandler(enabled = !isSearchActive && !isMoveMode && currentFolderId != null) {
        folderNavDirection = -1
        currentFolderId = noteById[currentFolderId]
            ?.parentNodeId
            ?.let { parentIdByNodeId[it] }
    }

    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is NoteListViewModel.UiEvent.ScrollToTop -> {
                    L.d(TAG, "UiEvent: ScrollToTop")
                    if (noteList.isNotEmpty()) {
                        latestActiveListState.scrollToItem(0)
                    }
                }
            }
        }
    }

    LaunchedEffect(currentFolderId, isSearchActive, currentNormalListState) {
        if (isSearchActive) return@LaunchedEffect
        snapshotFlow {
            currentNormalListState.firstVisibleItemIndex to
                    currentNormalListState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            viewModel.updateFolderScrollPosition(currentFolderId, index, offset)
        }
    }

    val centerContentVisible by remember(activeListState) {
        derivedStateOf {
            activeListState.firstVisibleItemIndex == 0 && activeListState.firstVisibleItemScrollOffset == 0
        }
    }
    val activeDisplayCount = remember(
        noteList,
        noteById,
        sortedChildrenByParent,
        currentFolderId,
        searchQuery
    ) {
        viewModel.visibleItemsForFolder(
            noteList = noteList,
            noteById = noteById,
            sortedChildrenByParent = sortedChildrenByParent,
            folderId = currentFolderId,
            searchQuery = searchQuery
        ).size
    }
    val showBackToTopFab by remember(activeListState, activeDisplayCount) {
        derivedStateOf {
            if (activeDisplayCount < BACK_TO_TOP_MIN_ITEMS) {
                false
            } else {
                val viewportHeight = activeListState.layoutInfo.viewportSize.height
                if (viewportHeight <= 0) {
                    false
                } else {
                    val firstVisibleSize =
                        activeListState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
                    if (firstVisibleSize <= 0) {
                        false
                    } else {
                        val approxScrollPx =
                            activeListState.firstVisibleItemIndex * firstVisibleSize +
                                    activeListState.firstVisibleItemScrollOffset
                        approxScrollPx >= viewportHeight * 2
                    }
                }
            }
        }
    }

    val currentFolder = remember(noteById, currentFolderId) { noteById[currentFolderId] }
    val navigateToFolder: (Int, Boolean) -> Unit = { folderId, fromSearchResult ->
        folderNavDirection = 1
        currentFolderId = folderId
        if (fromSearchResult) {
            viewModel.setSearchActive(false)
        }
    }

    suspend fun authenticateForSensitiveFolderAccess(): Boolean {
        val frequency = settingsRepository.getSensitiveNoteAuthFrequency()
        when (frequency) {
            SensitiveNoteAuthFrequency.NEVER -> return true
            SensitiveNoteAuthFrequency.ONCE_PER_APP_START -> {
                if (sensitiveAuthSessionStore.sensitiveNoteAuthPassedInSession) return true
            }

            SensitiveNoteAuthFrequency.EVERY_TIME -> Unit
        }

        // Keep parity with NoteDetail/open flow: if auth capability is unavailable, allow opening.
        if (!platform.isSensitiveAuthAvailable()) return true

        val authenticated = withTimeoutOrNull(20_000L) {
            platform.authenticateSensitiveToggle(openPromptTitle, openPromptSubtitle)
        } ?: false
        if (authenticated && frequency == SensitiveNoteAuthFrequency.ONCE_PER_APP_START) {
            sensitiveAuthSessionStore.sensitiveNoteAuthPassedInSession = true
        }
        return authenticated
    }

    val movingItemOriginalParentId = remember(noteList, movingItemId) {
        movingItemId
            ?.let { id -> noteById[id]?.parentNodeId }
            ?.let { parentNodeId -> parentIdByNodeId[parentNodeId] }
    }
    val canConfirmMove =
        remember(isMoveMode, movingItemId, movingItemOriginalParentId, currentFolderId) {
            isMoveMode && movingItemId != null && movingItemOriginalParentId != currentFolderId
        }
    val isMoveFabEnabled = !isMoveMode || canConfirmMove
    val moveFabContainerColor = if (isMoveFabEnabled) com.worldtheater.archive.ui.widget.capsuleBgColor() else com.worldtheater.archive.ui.theme.appSurfaceA45()
    val moveFabContentColor = if (isMoveFabEnabled) com.worldtheater.archive.ui.widget.capsuleContentColor() else com.worldtheater.archive.ui.theme.appOnSurfaceA45()

    val fabIcon = if (isMoveMode) Icons.Filled.Check else Icons.Filled.Add
    val fabDescription = if (isMoveMode) {
        confirmMoveDescription
    } else {
        addNoteDescription
    }

    NoteListScreenContent(
        modifier = modifier,
        isSearchActive = isSearchActive,
        isMoveMode = isMoveMode,
        showBackToTopFab = showBackToTopFab,
        isMoveFabEnabled = isMoveFabEnabled,
        fabIcon = fabIcon,
        fabDescription = fabDescription,
        backToTopDescription = backToTopDescription,
        onFabClick = {
            if (isMoveMode) {
                if (!canConfirmMove) return@NoteListScreenContent
                platform.vibrate(50, null)
                val movingId = movingItemId ?: return@NoteListScreenContent
                when (viewModel.moveItem(movingId, currentFolderId)) {
                    NoteListViewModel.MoveItemResult.SUCCESS -> {
                        movingItemId = null
                    }

                    NoteListViewModel.MoveItemResult.DEPTH_LIMIT -> {
                        platform.showMessage(folderDepthLimitMessage)
                    }

                    NoteListViewModel.MoveItemResult.CHILD_LIMIT -> {
                        platform.showMessage(folderItemsLimitMessage)
                    }

                    else -> {
                        platform.showMessage(moveFailedMessage)
                    }
                }
            } else {
                platform.vibrate(50, null)
                onAddNoteClick(currentFolderId)
            }
        },
        onBackToTopClick = {
            scope.launch {
                activeListState.scrollToItem(0)
            }
        },
        moveFabContainerColor = moveFabContainerColor,
        moveFabContentColor = moveFabContentColor,
        moveModeTitle = moveModeTitle,
        moveModeHint = moveModeHint,
        mainContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                MyPullToRefreshBox(
                    isRefreshing = false,
                    threshold = 160.dp,
                    onRefresh = {
                        platform.vibrate(50, null)
                        dialogState = NoteListDialogState.Create
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    FolderListContent(
                        viewModel = viewModel,
                        noteList = noteList,
                        noteById = noteById,
                        sortedChildrenByParent = sortedChildrenByParent,
                        currentFolderId = currentFolderId,
                        isSearchActive = isSearchActive,
                        searchQuery = searchQuery,
                        folderNavDirection = folderNavDirection,
                        searchListState = searchListState,
                        normalListStateForFolder = { folderId ->
                            folderListStates.getOrPut(folderId) {
                                val saved = viewModel.getFolderScrollPosition(folderId)
                                LazyListState(saved.index, saved.offset)
                            }
                        },
                        contentTopPadding = contentTopPadding,
                        isMoveMode = isMoveMode,
                        movingItemId = movingItemId,
                        openContextMenuNoteId = openContextMenuNoteId,
                        onEnterFolder = { folderId, fromSearchResult ->
                            val folder = noteById[folderId]
                            if (folder?.isSensitive == true) {
                                scope.launch {
                                    if (authenticateForSensitiveFolderAccess()) {
                                        navigateToFolder(folderId, fromSearchResult)
                                    }
                                }
                            } else {
                                navigateToFolder(folderId, fromSearchResult)
                            }
                        },
                        onOpenNote = { noteId ->
                            val now = elapsedRealtimeMillis()
                            if (now - lastNavigateAt < 600L) return@FolderListContent
                            val targetNote = noteById[noteId]
                            scope.launch {
                                val canOpen = targetNote == null || !targetNote.isSensitive ||
                                    authenticateForSensitiveFolderAccess()
                                if (!canOpen) return@launch
                                if (!isSearchActive) {
                                    viewModel.updateFolderScrollPosition(
                                        currentFolderId,
                                        currentNormalListState.firstVisibleItemIndex,
                                        currentNormalListState.firstVisibleItemScrollOffset
                                    )
                                }
                                lastNavigateAt = elapsedRealtimeMillis()
                                onNoteClick(noteId)
                            }
                        },
                        onDeleteClick = {
                            dialogState = NoteListDialogState.Delete(it)
                        },
                        onDetailsClick = {
                            dialogState = NoteListDialogState.ItemDetails(it)
                        },
                        onMoveClick = {
                            movingItemId = it._id
                            openContextMenuNoteId = null
                            viewModel.setSearchActive(false)
                        },
                        onToggleSensitiveClick = {
                            if (!it.isSensitive && it.title.isBlank()) {
                                dialogState = NoteListDialogState.RequireTitleForLock(it)
                                openContextMenuNoteId = null
                            } else {
                                scope.launch {
                                    val authenticated =
                                        if (platform.shouldBlockSensitiveAuth()) {
                                            false
                                        } else {
                                            platform.authenticateSensitiveToggle(
                                                if (it.isSensitive) unlockPromptTitle else lockPromptTitle,
                                                if (it.isSensitive) unlockPromptSubtitle else lockPromptSubtitle
                                            )
                                        }
                                    if (authenticated) {
                                        platform.vibrate(
                                            if (it.isSensitive) 12L else 24L,
                                            if (it.isSensitive) 50 else 180
                                        )
                                        viewModel.updateNoteSensitive(
                                            note = it,
                                            isSensitive = !it.isSensitive
                                        )
                                    }
                                }
                            }
                        },
                        onMenuVisibilityChange = { noteId, show ->
                            if (show) {
                                val now = elapsedRealtimeMillis()
                                if (now - lastLongPressAt < 400L) return@FolderListContent
                                lastLongPressAt = now
                                openContextMenuNoteId = noteId
                            } else if (openContextMenuNoteId == noteId) {
                                openContextMenuNoteId = null
                            }
                        },
                        onLongPressFeedback = {
                            platform.vibrate(10, null)
                        }
                    )
                }
            }
        },
        topBarContent = {
            NoteListTopBarLayer(
                isSearchActive = isSearchActive,
                modifier = Modifier.align(Alignment.TopCenter),
                normalContent = {
                    NormalNoteListTopBarContent(
                        totalNotesText = totalNotesFormatter(totalNotesCount),
                        currentFolderName = currentFolder?.title,
                        hasParentFolder = !currentFolder?.title.isNullOrBlank(),
                        centerContentVisible = centerContentVisible,
                        onSettingsClick = onSettingsClick,
                        onStrongPasswordGeneratorClick = {
                            showStrongPasswordDialog = true
                        },
                        onClearClipboardClick = {
                            platform.clearClipboard()
                            platform.showMessage(clipboardClearedMessage)
                        },
                        onCreateFolderClick = {
                            dialogState = NoteListDialogState.CreateFolder
                        },
                        isMoveMode = isMoveMode,
                        onExitMoveMode = { movingItemId = null },
                        onNavigationClick = {
                            if (isMoveMode || !currentFolder?.title.isNullOrBlank()) {
                                folderNavDirection = -1
                                currentFolderId = currentFolder
                                    ?.parentNodeId
                                    ?.let { parentIdByNodeId[it] }
                            } else {
                                viewModel.setSearchActive(true)
                            }
                        }
                    )
                },
                searchContent = {
                    NoteListSearchTopAppBarContent(
                        searchQuery = searchQuery,
                        onBack = { viewModel.setSearchActive(false) },
                        onSearchChange = { viewModel.search(it) },
                        onClear = { viewModel.search("") }
                    )
                }
            )
        },
        dialogContent = {
            when (val state = dialogState) {
                NoteListDialogState.Create -> {
                    com.worldtheater.archive.feature.note_list.components.dialogs.NewNoteDialog(
                        title = newNoteDialogTitle,
                        primaryText = actionOk,
                        secondaryText = actionCancel,
                        defaultPlaceholderText = writeSomething,
                        onDismiss = { dialogState = NoteListDialogState.None },
                        onNegativeClick = { dialogState = NoteListDialogState.None },
                        onPositiveClick = { text ->
                            if (text.isNotBlank()) {
                                viewModel.addNote(text, currentFolderId)
                                platform.vibrate(50, null)
                            }
                            dialogState = NoteListDialogState.None
                        }
                    )
                }

                NoteListDialogState.CreateFolder -> {
                    CreateFolderDialog(
                        maxLength = MAX_FOLDER_NAME_LENGTH,
                        onDismiss = { dialogState = NoteListDialogState.None },
                        onConfirm = { trimmed ->
                            when (viewModel.addFolder(trimmed, currentFolderId)) {
                                NoteListViewModel.AddFolderResult.SUCCESS -> {
                                    platform.vibrate(50, null)
                                }

                                NoteListViewModel.AddFolderResult.DEPTH_LIMIT -> {
                                    platform.showMessage(folderDepthLimitMessage)
                                }

                                NoteListViewModel.AddFolderResult.CHILD_LIMIT -> {
                                    platform.showMessage(folderItemsLimitMessage)
                                }

                                NoteListViewModel.AddFolderResult.TOTAL_LIMIT -> {
                                    platform.showMessage(totalItemsLimitMessage)
                                }
                            }
                            dialogState = NoteListDialogState.None
                        }
                    )
                }

                is NoteListDialogState.Delete -> {
                    val isFolder = state.note.itemType == ITEM_TYPE_FOLDER
                    DeleteConfirmDialog(
                        title = if (isFolder) {
                            stringResource(Res.string.msg_confirm_delete_folder)
                        } else {
                            stringResource(Res.string.msg_confirm_delete)
                        },
                        description = if (isFolder) {
                            stringResource(Res.string.msg_delete_folder_irreversible_fmt, viewModel.countDescendants(noteList, state.note._id))
                        } else {
                            stringResource(Res.string.msg_delete_irreversible)
                        },
                        onDismiss = { dialogState = NoteListDialogState.None },
                        onConfirm = {
                            viewModel.deleteNote(state.note)
                            dialogState = NoteListDialogState.None
                        }
                    )
                }

                is NoteListDialogState.RequireTitleForLock -> {
                    RequireTitleForLockDialog(
                        key = "lock_title_${state.note._id}",
                        maxLength = NOTE_TITLE_MAX_LENGTH,
                        onDismiss = { dialogState = NoteListDialogState.None },
                        onConfirm = { trimmed ->
                            dialogState = NoteListDialogState.None
                            scope.launch {
                                val authenticated =
                                    if (platform.shouldBlockSensitiveAuth()) {
                                        false
                                    } else {
                                        platform.authenticateSensitiveToggle(
                                            lockPromptTitle,
                                            lockPromptSubtitle
                                        )
                                    }
                                if (authenticated) {
                                    platform.vibrate(24L, 180)
                                    viewModel.updateNote(
                                        state.note.copy(
                                            title = trimmed,
                                            isSensitive = true
                                        )
                                    )
                                }
                            }
                        }
                    )
                }

                is NoteListDialogState.ItemDetails -> {
                    val item = state.item
                    val itemPath = viewModel.buildItemPath(noteList, item)
                    val actualCharCount by produceState(
                        initialValue = item.body.length,
                        key1 = item._id,
                        key2 = item.isBodyPreview,
                        key3 = item.body.length
                    ) {
                        if (!item.isSensitive && item.isBodyPreview && item._id > 0) {
                            value = viewModel.getNoteById(item._id)?.body?.length ?: item.body.length
                        } else {
                            value = item.body.length
                        }
                    }
                    val details = buildItemDetailsRows(
                        item = item,
                        noteList = noteList,
                        itemPath = itemPath,
                        actualCharCount = actualCharCount,
                        descendantCount = viewModel.countDescendants(noteList, item._id),
                        createdAtText = dateTimeFormatter.formatDateTime(item.createTime),
                        updatedAtText = dateTimeFormatter.formatDateTime(item.updateTime)
                    )
                    ItemDetailsDialog(
                        title = detailsTitle,
                        details = details,
                        onDismiss = { dialogState = NoteListDialogState.None }
                    )
                }

                NoteListDialogState.None -> {}
            }

            if (showStrongPasswordDialog) {
                com.worldtheater.archive.feature.note_list.components.dialogs.StrongPasswordGeneratorDialog(
                    titleText = strongPasswordTitle,
                    passwordLabel = passwordLabel,
                    copyPasswordContentDescription = copyPasswordDescription,
                    regeneratePasswordText = regeneratePasswordText,
                    passwordLengthFormatter = passwordLengthFormatter,
                    passwordStrengthFormatter = passwordStrengthFormatter,
                    onDismiss = { showStrongPasswordDialog = false },
                    onCopyPassword = { label, password ->
                        platform.copyToClipboard(label, password)
                        platform.showMessage(passwordCopiedMessage)
                    }
                )
            }
        }
    )
}

@Composable
private fun rememberNoteListPlatformActions(): NoteListPlatformActions {
    val availabilityProvider: SensitiveAuthAvailabilityProvider = koinInject()
    val authPrompt: SensitiveAuthPrompt = koinInject()
    val messageSink: UserMessageSink = koinInject()
    val hapticFeedbackGateway: HapticFeedbackGateway = koinInject()
    val clipboardGateway: ClipboardGateway = koinInject()

    val noCredentialText = stringResource(Res.string.msg_sensitive_auth_setup_required)
    val noHardwareText = stringResource(Res.string.pref_sensitive_note_auth_unavailable_no_hardware)
    val hardwareUnavailableText = stringResource(Res.string.pref_sensitive_note_auth_unavailable_hw_unavailable)
    val unavailableGenericText = stringResource(Res.string.pref_sensitive_note_auth_unavailable_generic)

    return NoteListPlatformActions(
        vibrate = { milliseconds, effect ->
            hapticFeedbackGateway.vibrate(milliseconds, effect)
        },
        showMessage = messageSink::showShort,
        clearClipboard = clipboardGateway::clear,
        copyToClipboard = clipboardGateway::copy,
        isSensitiveAuthAvailable = { availabilityProvider.isAvailable() },
        shouldBlockSensitiveAuth = {
            when (availabilityProvider.availability()) {
                SensitiveAuthAvailability.AVAILABLE -> false
                SensitiveAuthAvailability.NO_CREDENTIAL -> {
                    messageSink.showShort(noCredentialText)
                    true
                }

                SensitiveAuthAvailability.NO_HARDWARE -> {
                    messageSink.showShort(noHardwareText)
                    true
                }

                SensitiveAuthAvailability.HARDWARE_UNAVAILABLE -> {
                    messageSink.showShort(hardwareUnavailableText)
                    true
                }

                SensitiveAuthAvailability.UNAVAILABLE -> {
                    messageSink.showShort(unavailableGenericText)
                    true
                }
            }
        },
        authenticateSensitiveToggle = { title, subtitle ->
            authPrompt.authenticate(title = title, subtitle = subtitle)
        }
    )
}
