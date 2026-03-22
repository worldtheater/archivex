package com.worldtheater.archive.feature.note_list.screens

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import archivex.composeapp.generated.resources.*
import com.worldtheater.archive.domain.BackupSecurityMode
import com.worldtheater.archive.domain.model.ITEM_TYPE_FOLDER
import com.worldtheater.archive.domain.model.MAX_FOLDER_DEPTH
import com.worldtheater.archive.domain.model.MAX_FOLDER_DIRECT_ITEMS
import com.worldtheater.archive.domain.model.MAX_TOTAL_ITEMS
import com.worldtheater.archive.domain.model.NoteInfo
import com.worldtheater.archive.domain.model.NoteLimitException
import com.worldtheater.archive.domain.model.NoteLimitType
import com.worldtheater.archive.domain.repository.NoteRepository
import com.worldtheater.archive.domain.repository.SettingsRepository
import com.worldtheater.archive.domain.usecase.NoteTreeOps
import com.worldtheater.archive.platform.auth.DeviceAuthAvailabilityChecker
import com.worldtheater.archive.platform.gateway.TransferFileGateway
import com.worldtheater.archive.platform.system.AppFeatureFlags
import com.worldtheater.archive.platform.system.UserMessageSink
import com.worldtheater.archive.platform.system.currentTimeMillis
import com.worldtheater.archive.platform.system.generateUuidString
import com.worldtheater.archive.ui.theme.COLORS
import com.worldtheater.archive.util.log.L
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

class NoteListViewModel(
    private val repo: NoteRepository,
    private val settingsRepository: SettingsRepository,
    private val fileGateway: TransferFileGateway,
    private val userMessageSink: UserMessageSink,
    private val deviceAuthAvailabilityChecker: DeviceAuthAvailabilityChecker,
    private val appFeatureFlags: AppFeatureFlags,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val guidePresetDataEnabled: Boolean = appFeatureFlags.guidePresetDataEnabled,
    private val canSeedSensitiveGuideNote: () -> Boolean = {
        deviceAuthAvailabilityChecker.isAvailable()
    },
    private val guidePresetNotesFactory: ((Boolean) -> List<NoteInfo>)? = null,
) : ViewModel() {
    data class ListScrollPosition(val index: Int = 0, val offset: Int = 0)
    enum class AddFolderResult {
        SUCCESS,
        DEPTH_LIMIT,
        CHILD_LIMIT,
        TOTAL_LIMIT
    }

    enum class MoveItemResult {
        SUCCESS,
        INVALID,
        DEPTH_LIMIT,
        CYCLE_DETECTED,
        CHILD_LIMIT
    }

    enum class TransferType {
        BACKUP,
        RESTORE,
        EXPORT_PLAIN,
        IMPORT_PLAIN
    }

    data class TransferProgress(
        val type: TransferType,
        val completedSteps: Int,
        val totalSteps: Int
    )

    private var _currentQuery = ""
    private var searchJob: Job? = null
    private var allNotesJob: Job? = null
    private var guidePresetDataJob: Job? = null
    private var allNotesCache: List<NoteInfo> = emptyList()
    private var searchSession: Long = 0L

    private val _state = mutableStateOf<List<NoteInfo>>(emptyList())
    val state: State<List<NoteInfo>> = _state
    private val _totalNotesCount = mutableStateOf(0)
    val totalNotesCount: State<Int> = _totalNotesCount

    private val _isSearchActive = mutableStateOf(false)
    val isSearchActive: State<Boolean> = _isSearchActive

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    sealed class UiEvent {
        object ScrollToTop : UiEvent()
    }

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()
    private val folderScrollPositions = mutableMapOf<Int?, ListScrollPosition>()
    private val transferMutex = Mutex()
    private val _transferProgress = MutableStateFlow<TransferProgress?>(null)
    val transferProgress: StateFlow<TransferProgress?> = _transferProgress

    private fun showToast(message: String) {
        viewModelScope.launch(mainDispatcher) {
            userMessageSink.showShort(message)
        }
    }

    fun getNotes() {
        seedGuidePresetDataIfNeeded()
        collectAllNotes()
    }

    fun clearNotes() {
        allNotesJob?.cancel()
        searchJob?.cancel()
        _state.value = emptyList()
    }

    fun addNote(content: String, parentId: Int? = null) {
        val parentNodeId = parentId?.let { NoteTreeOps.parentNodeIdById(it, _state.value) }
        addNote(
            NoteInfo(
                body = content,
                createTime = currentTimeMillis(),
                updateTime = currentTimeMillis(),
                color = randomNoteColor(),
                parentNodeId = parentNodeId
            )
        )
    }

    fun addNote(
        title: String,
        content: String,
        parentId: Int? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        val parentNodeId = parentId?.let { NoteTreeOps.parentNodeIdById(it, _state.value) }
        val now = currentTimeMillis()
        addNote(
            NoteInfo(
                title = title.trim(),
                body = content,
                createTime = now,
                updateTime = now,
                color = randomNoteColor(),
                parentNodeId = parentNodeId
            ),
            onSuccess = onSuccess
        )
    }

    suspend fun getNoteById(id: Int): NoteInfo? {
        return repo.getNoteById(id).firstOrNull()
    }

    suspend fun getNoteForViewById(id: Int): NoteInfo? {
        return repo.getNoteForViewById(id).firstOrNull()
    }

    fun addFolder(name: String, parentId: Int?): AddFolderResult {
        val parentNodeId = parentId?.let { NoteTreeOps.parentNodeIdById(it, _state.value) }
        if (_state.value.size >= MAX_TOTAL_ITEMS) return AddFolderResult.TOTAL_LIMIT
        if (NoteTreeOps.computeDepth(parentNodeId, _state.value) >= MAX_FOLDER_DEPTH) {
            return AddFolderResult.DEPTH_LIMIT
        }
        if (parentNodeId != null) {
            val childCount = _state.value.count { it.parentNodeId == parentNodeId }
            if (childCount >= MAX_FOLDER_DIRECT_ITEMS) {
                return AddFolderResult.CHILD_LIMIT
            }
        }
        val now = currentTimeMillis()
        addNote(
            NoteInfo(
                title = name.trim(),
                body = "",
                createTime = now,
                updateTime = now,
                color = randomNoteColor(),
                itemType = ITEM_TYPE_FOLDER,
                parentNodeId = parentNodeId
            )
        )
        return AddFolderResult.SUCCESS
    }

    fun addNote(note: NoteInfo, onSuccess: (() -> Unit)? = null) {
        L.d(TAG, "addNote: $note")
        viewModelScope.launch {
            try {
                repo.addNote(note)
                onSuccess?.invoke()
                _uiEvent.trySend(UiEvent.ScrollToTop)
            } catch (e: NoteLimitException) {
                showLimitToast(e)
            }
        }
    }

    fun deleteNote(note: NoteInfo) {
        L.d(TAG, "deleteNote: $note")
        viewModelScope.launch {
            repo.deleteNote(note)
        }
    }

    fun updateNote(note: NoteInfo) {
        L.d(TAG, "updateNote: $note")
        viewModelScope.launch {
            val updatedNote = note.copy(
                updateTime = currentTimeMillis()
            )
            try {
                repo.updateNote(updatedNote)
            } catch (e: NoteLimitException) {
                showLimitToast(e)
            }
        }
    }

    fun updateNoteSensitive(note: NoteInfo, isSensitive: Boolean) {
        L.d(TAG, "updateNoteSensitive: id=${note._id}, isSensitive=$isSensitive")
        if (note._id <= 0) return
        viewModelScope.launch {
            repo.updateNoteSensitive(
                id = note._id,
                isSensitive = isSensitive,
                updateTime = currentTimeMillis()
            )
        }
    }

    fun moveItem(itemId: Int, targetParentId: Int?): MoveItemResult {
        val item = _state.value.find { it._id == itemId } ?: return MoveItemResult.INVALID
        val targetParentNodeId = targetParentId?.let {
            NoteTreeOps.parentNodeIdById(it, _state.value)
        }
        if (item._id == targetParentId) return MoveItemResult.INVALID
        val targetDepth = NoteTreeOps.computeDepth(targetParentNodeId, _state.value)
        if (targetDepth >= MAX_FOLDER_DEPTH) return MoveItemResult.DEPTH_LIMIT
        if (targetParentNodeId != item.parentNodeId && targetParentNodeId != null) {
            val directChildrenCount = _state.value.count {
                it.parentNodeId == targetParentNodeId && it._id != itemId
            }
            if (directChildrenCount >= MAX_FOLDER_DIRECT_ITEMS) {
                return MoveItemResult.CHILD_LIMIT
            }
        }
        if (item.itemType == ITEM_TYPE_FOLDER && targetParentId != null) {
            if (NoteTreeOps.isDescendant(
                    descendantNodeId = targetParentNodeId ?: return MoveItemResult.INVALID,
                    ancestorNodeId = item.nodeId,
                    notes = _state.value
                )
            ) {
                return MoveItemResult.CYCLE_DETECTED
            }
            val subtreeHeight = NoteTreeOps.computeSubtreeHeight(item._id, _state.value)
            if (targetDepth + subtreeHeight > MAX_FOLDER_DEPTH) {
                return MoveItemResult.DEPTH_LIMIT
            }
        }
        viewModelScope.launch {
            try {
                repo.updateNote(
                    item.copy(
                        parentNodeId = targetParentNodeId,
                        updateTime = currentTimeMillis()
                    )
                )
            } catch (e: NoteLimitException) {
                showLimitToast(e)
            }
        }
        return MoveItemResult.SUCCESS
    }

    fun countDescendants(noteList: List<NoteInfo>, rootFolderId: Int): Int {
        return NoteTreeOps.countDescendants(noteList, rootFolderId)
    }

    fun buildItemPath(noteList: List<NoteInfo>, item: NoteInfo): String {
        return NoteTreeOps.buildItemPath(noteList, item)
    }

    fun buildNoteById(noteList: List<NoteInfo>): Map<Int, NoteInfo> {
        return NoteTreeOps.buildNoteById(noteList)
    }

    fun buildParentIdByNodeId(noteList: List<NoteInfo>): Map<String, Int> {
        return NoteTreeOps.buildParentIdByNodeId(noteList)
    }

    fun buildSortedChildrenByParent(noteList: List<NoteInfo>): Map<String?, List<NoteInfo>> {
        return NoteTreeOps.buildSortedChildrenByParent(noteList)
    }

    fun visibleItemsForFolder(
        noteList: List<NoteInfo>,
        noteById: Map<Int, NoteInfo>,
        sortedChildrenByParent: Map<String?, List<NoteInfo>>,
        folderId: Int?,
        searchQuery: String
    ): List<NoteInfo> {
        return NoteTreeOps.visibleItemsForFolder(
            noteList = noteList,
            noteById = noteById,
            sortedChildrenByParent = sortedChildrenByParent,
            folderId = folderId,
            searchQuery = searchQuery
        )
    }

    private fun showLimitToast(e: NoteLimitException) {
        when (e.type) {
            NoteLimitType.TOTAL_ITEMS -> {
                showToast(s(Res.string.msg_total_items_limit_fmt, e.limit))
            }

            NoteLimitType.NOTE_BODY_LENGTH -> {
                showToast(s(Res.string.msg_note_body_too_long_fmt, e.limit))
            }

            NoteLimitType.FOLDER_DIRECT_ITEMS -> {
                showToast(s(Res.string.msg_folder_items_limit_fmt, e.limit))
            }
        }
    }

    private fun tryStartTransfer(type: TransferType): Boolean {
        if (!transferMutex.tryLock()) {
            showToast(s(Res.string.msg_transfer_in_progress))
            return false
        }
        _transferProgress.value = TransferProgress(type = type, completedSteps = 0, totalSteps = 0)
        return true
    }

    private fun updateTransferProgress(type: TransferType, completedSteps: Int, totalSteps: Int) {
        _transferProgress.value = TransferProgress(
            type = type,
            completedSteps = completedSteps.coerceAtLeast(0),
            totalSteps = totalSteps.coerceAtLeast(0)
        )
    }

    private fun finishTransfer() {
        _transferProgress.value = null
        if (transferMutex.isLocked) {
            transferMutex.unlock()
        }
    }

    fun backup(
        uriString: String,
        pwd: String,
        mode: BackupSecurityMode = BackupSecurityMode.CROSS_DEVICE
    ) {
        if (!tryStartTransfer(TransferType.BACKUP)) return
        L.d(TAG, "backup: uri=$uriString")
        viewModelScope.launch(ioDispatcher) {
            try {
                val bytes = repo.backup(pwd, mode) { completed, total ->
                    updateTransferProgress(TransferType.BACKUP, completed, total)
                }
                fileGateway.writeBytes(uriString, bytes)
                showToast(s(Res.string.msg_backup_success))
            } catch (e: Exception) {
                L.e(TAG, "backup failed", e)
                val errorMsg = e.message ?: s(Res.string.msg_unknown_error)
                showToast(s(Res.string.msg_backup_failure_fmt, errorMsg))
            } finally {
                finishTransfer()
            }
        }
    }

    fun exportPlain(uriString: String) {
        if (!tryStartTransfer(TransferType.EXPORT_PLAIN)) return
        viewModelScope.launch(ioDispatcher) {
            try {
                val bytes = repo.exportPlain { completed, total ->
                    updateTransferProgress(TransferType.EXPORT_PLAIN, completed, total)
                }
                fileGateway.writeBytes(uriString, bytes)
                showToast(s(Res.string.msg_plain_export_success))
            } catch (e: Exception) {
                L.e(TAG, "exportPlain failed", e)
                val errorMsg = e.message ?: s(Res.string.msg_unknown_error)
                showToast(s(Res.string.msg_plain_export_failure_fmt, errorMsg))
            } finally {
                finishTransfer()
            }
        }
    }

    fun importPlainAsNew(uriString: String) {
        if (!tryStartTransfer(TransferType.IMPORT_PLAIN)) return
        viewModelScope.launch(ioDispatcher) {
            try {
                val inputBytes = fileGateway.readBytes(uriString)
                repo.importPlainAsNew(inputBytes) { completed, total ->
                    updateTransferProgress(TransferType.IMPORT_PLAIN, completed, total)
                }
                collectAllNotes()
                showToast(s(Res.string.msg_plain_import_success))
            } catch (e: NoteLimitException) {
                showLimitToast(e)
            } catch (e: Exception) {
                L.e(TAG, "importPlainAsNew failed", e)
                val errorMsg = e.message ?: s(Res.string.msg_unknown_error)
                showToast(s(Res.string.msg_plain_import_failure_fmt, errorMsg))
            } finally {
                finishTransfer()
            }
        }
    }

    fun restore(uriString: String, pwd: String) {
        if (!tryStartTransfer(TransferType.RESTORE)) return
        L.d(TAG, "restore: uri=$uriString")
        viewModelScope.launch(ioDispatcher) {
            try {
                val inputBytes = fileGateway.readBytes(uriString)
                repo.restore(pwd, inputBytes) { completed, total ->
                    updateTransferProgress(TransferType.RESTORE, completed, total)
                }

                collectAllNotes()
                showToast(s(Res.string.msg_restore_success))
            } catch (e: NoteLimitException) {
                showLimitToast(e)
            } catch (e: Exception) {
                L.e(TAG, "restore failed", e)
                val errorMsg = e.message ?: s(Res.string.msg_unknown_error)
                showToast(s(Res.string.msg_restore_failure_fmt, errorMsg))
            } finally {
                finishTransfer()
            }
        }
    }

    fun backupToDirectory(
        pwd: String,
        mode: BackupSecurityMode = BackupSecurityMode.CROSS_DEVICE
    ) {
        if (!tryStartTransfer(TransferType.BACKUP)) return
        val dirUriStr = settingsRepository.getBackupDir()
        if (dirUriStr == null) {
            showToast(s(Res.string.msg_backup_no_dir))
            finishTransfer()
            return
        }

        viewModelScope.launch(ioDispatcher) {
            var newFileUriString: String? = null
            try {
                val filename = settingsRepository.generateBackupFilename()
                newFileUriString = fileGateway.createFile(
                    treeUriString = dirUriStr,
                    mimeType = "application/octet-stream",
                    displayName = filename
                )

                val bytes = repo.backup(pwd, mode) { completed, total ->
                    updateTransferProgress(TransferType.BACKUP, completed, total)
                }
                fileGateway.writeBytes(newFileUriString, bytes)

                settingsRepository.setLastBackupInfo(
                    filename,
                    currentTimeMillis()
                )
                val msg = s(Res.string.msg_backup_success) + ": $filename"
                showToast(msg)
            } catch (e: Exception) {
                L.e(TAG, "backupToDirectory failed", e)
                val errorMsg = e.message ?: s(Res.string.msg_unknown_error)
                showToast(s(Res.string.msg_backup_failure_fmt, errorMsg))
                try {
                    newFileUriString?.let { fileGateway.deleteFile(it) }
                } catch (deleteEx: Exception) {
                    L.e(TAG, "Failed to delete empty backup file", deleteEx)
                }
            } finally {
                finishTransfer()
            }
        }
    }

    fun search(query: String) {
        viewModelScope.launch(mainDispatcher) {
            performSearch(query)
        }
    }

    private suspend fun performSearch(query: String) {
        ensureAllNotesCollectionStarted()
        _currentQuery = query
        _searchQuery.value = query

        // Cancel previous job to avoid race conditions or redundant collections
        searchJob?.cancel()
        val session = ++searchSession

        searchJob = viewModelScope.launch {
            if (session != searchSession || _searchQuery.value != query) return@launch
            val needle = query.trim()
            if (needle.isEmpty()) {
                updateStateIfChanged(allNotesCache)
            } else {
                repo.searchNotes(needle).collect { result ->
                    if (session != searchSession || _searchQuery.value != query) return@collect
                    updateStateIfChanged(filterSearchResult(result))
                }
            }
        }
    }

    private fun filterSearchResult(result: List<NoteInfo>): List<NoteInfo> {
        if (result.isEmpty() || allNotesCache.isEmpty()) return result
        val sensitiveFolderNodeIds = allNotesCache.asSequence()
            .filter { it.itemType == ITEM_TYPE_FOLDER && it.isSensitive }
            .map { it.nodeId }
            .filter { it.isNotBlank() }
            .toSet()
        if (sensitiveFolderNodeIds.isEmpty()) return result
        val byNodeId = allNotesCache.associateBy { it.nodeId }
        return result.filterNot { item ->
            var parentNodeId = item.parentNodeId
            val visited = mutableSetOf<String>()
            while (parentNodeId != null && visited.add(parentNodeId)) {
                if (parentNodeId in sensitiveFolderNodeIds) {
                    return@filterNot true
                }
                parentNodeId = byNodeId[parentNodeId]?.parentNodeId
            }
            false
        }
    }

    private fun updateStateIfChanged(newState: List<NoteInfo>) {
        if (_state.value != newState) {
            _state.value = newState
        }
    }

    private fun seedGuidePresetDataIfNeeded() {
        if (!guidePresetDataEnabled) return
        if (guidePresetDataJob?.isActive == true) return
        guidePresetDataJob = viewModelScope.launch(ioDispatcher) {
            try {
                if (settingsRepository.isGuidePresetDataSeeded()) return@launch
                val existing = repo.getRecentNotesForList(limit = 1)
                if (existing.isNotEmpty()) {
                    settingsRepository.setGuidePresetDataSeeded(true)
                    return@launch
                }
                val includeSensitiveSample = canSeedSensitiveGuideNote()
                val presetNotes = guidePresetNotesFactory?.invoke(includeSensitiveSample)
                    ?: buildGuidePresetNotes(includeSensitiveSample)
                repo.addNotesBatch(presetNotes)
                settingsRepository.setGuidePresetDataSeeded(true)
            } catch (e: Exception) {
                L.e(TAG, "seedGuidePresetDataIfNeeded failed", e)
            }
        }
    }

    private fun buildGuidePresetNotes(includeSensitiveSample: Boolean): List<NoteInfo> {
        val now = currentTimeMillis()
        val folderNodeId = generateUuidString()
        val moveTargetFolderNodeId = generateUuidString()
        val notesForDisplay = mutableListOf(
            NoteInfo(
                title = s(Res.string.preset_note_welcome_title),
                body = s(Res.string.preset_note_welcome_body),
                createTime = now - 6_000L,
                updateTime = now - 6_000L,
                color = presetNoteColor(0)
            ),
            NoteInfo(
                title = s(Res.string.preset_note_basic_ops_title),
                body = s(Res.string.preset_note_basic_ops_body),
                createTime = now - 5_000L,
                updateTime = now - 5_000L,
                color = presetNoteColor(1)
            ),
            NoteInfo(
                title = "",
                body = s(Res.string.preset_note_title_help_body),
                createTime = now - 4_000L,
                updateTime = now - 4_000L,
                color = presetNoteColor(5)
            ),
            NoteInfo(
                nodeId = folderNodeId,
                title = s(Res.string.preset_note_quick_start_folder_title),
                body = "",
                createTime = now - 3_000L,
                updateTime = now - 3_000L,
                color = presetNoteColor(2),
                itemType = ITEM_TYPE_FOLDER
            ),
            NoteInfo(
                nodeId = moveTargetFolderNodeId,
                title = s(Res.string.preset_note_move_target_folder_title),
                body = "",
                createTime = now - 2_500L,
                updateTime = now - 2_500L,
                color = presetNoteColor(6),
                itemType = ITEM_TYPE_FOLDER,
                parentNodeId = folderNodeId
            ),
            NoteInfo(
                title = s(Res.string.preset_note_move_to_folder_title),
                body = s(Res.string.preset_note_move_to_folder_body),
                createTime = now - 2_000L,
                updateTime = now - 2_000L,
                color = presetNoteColor(3),
                parentNodeId = folderNodeId
            )
        )
        notesForDisplay += if (includeSensitiveSample) {
            NoteInfo(
                title = s(Res.string.preset_note_private_title),
                body = s(Res.string.preset_note_private_body_locked),
                createTime = now - 1_000L,
                updateTime = now - 1_000L,
                color = presetNoteColor(4),
                isSensitive = true
            )
        } else {
            NoteInfo(
                title = s(Res.string.preset_note_private_title),
                body = s(Res.string.preset_note_private_body_unlocked),
                createTime = now - 1_000L,
                updateTime = now - 1_000L,
                color = presetNoteColor(4),
                isSensitive = false
            )
        }
        // Notes are queried by _id DESC, so we insert in reverse of desired reading order.
        return notesForDisplay.asReversed()
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            search("")
        }
    }

    fun getFolderScrollPosition(folderId: Int?): ListScrollPosition {
        return folderScrollPositions[folderId] ?: ListScrollPosition()
    }

    fun updateFolderScrollPosition(folderId: Int?, index: Int, offset: Int) {
        folderScrollPositions[folderId] = ListScrollPosition(index = index, offset = offset)
    }

    private fun collectAllNotes() {
        ensureAllNotesCollectionStarted()
        search("")
    }

    private fun ensureAllNotesCollectionStarted() {
        if (allNotesJob?.isActive == true) return
        allNotesJob = viewModelScope.launch {
            val notesCount = runCatching {
                repo.getNotesCount()
            }.onSuccess { count ->
                _totalNotesCount.value = count.coerceAtLeast(0)
            }.onFailure { e ->
                L.e(TAG, "load notes count failed", e)
            }.getOrNull()
            val shouldPreload = notesCount == null || notesCount >= INITIAL_LIST_PRELOAD_COUNT
            if (shouldPreload) {
                runCatching {
                    repo.getRecentNotesForListByParent(
                        parentNodeId = null,
                        limit = INITIAL_LIST_PRELOAD_COUNT
                    )
                }.onSuccess { initial ->
                    if (initial.isNotEmpty()) {
                        allNotesCache = initial
                        if (_totalNotesCount.value == 0) {
                            _totalNotesCount.value = initial.size
                        }
                        if (_searchQuery.value.trim().isEmpty()) {
                            updateStateIfChanged(initial)
                        }
                    }
                }.onFailure { e ->
                    L.e(TAG, "initial preload failed", e)
                }
            }
            repo.allNotes().collect { result ->
                allNotesCache = result
                _totalNotesCount.value = result.size
                if (_searchQuery.value.trim().isEmpty()) {
                    updateStateIfChanged(result)
                }
            }
        }
    }

    private fun randomNoteColor(): Int = COLORS.random().toArgb()

    private fun presetNoteColor(index: Int): Int = COLORS[index].toArgb()

    companion object {

        const val TAG = "NoteListViewModel"
        private const val INITIAL_LIST_PRELOAD_COUNT = 200

        internal fun s(resource: StringResource, vararg args: Any): String {
            return runBlocking { getString(resource, *args) }
        }
    }
}
