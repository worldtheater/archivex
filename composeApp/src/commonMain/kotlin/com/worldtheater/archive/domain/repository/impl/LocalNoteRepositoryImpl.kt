package com.worldtheater.archive.domain.repository.impl

import com.worldtheater.archive.data.local.Note
import com.worldtheater.archive.data.local.NoteDao
import com.worldtheater.archive.domain.BackupSecurityMode
import com.worldtheater.archive.domain.model.ITEM_TYPE_FOLDER
import com.worldtheater.archive.domain.model.MAX_FOLDER_DIRECT_ITEMS
import com.worldtheater.archive.domain.model.MAX_NOTE_BODY_LENGTH
import com.worldtheater.archive.domain.model.MAX_TOTAL_ITEMS
import com.worldtheater.archive.domain.model.NoteInfo
import com.worldtheater.archive.domain.model.NoteLimitException
import com.worldtheater.archive.domain.model.NoteLimitType
import com.worldtheater.archive.domain.repository.NoteRepository
import com.worldtheater.archive.platform.data.LocalNoteRepositoryPlatform
import com.worldtheater.archive.platform.system.currentTimeMillis
import com.worldtheater.archive.platform.system.generateUuidString
import com.worldtheater.archive.util.JsonUtils
import com.worldtheater.archive.util.log.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalNoteRepositoryImpl(
    private val deps: Deps = Deps.default()
) : NoteRepository {
    override suspend fun getAllNotes(): List<NoteInfo> {
        return toNoteInfos(noteDao().getAll())
    }

    override suspend fun allNotes(): Flow<List<NoteInfo>> {
        L.d(TAG, "allNotes")
        return noteDao().allNotesForList(LIST_BODY_PREVIEW_CHARS)
            .map { toNoteInfos(it, bodyPreview = true) }
    }

    override suspend fun getRecentNotesForList(limit: Int): List<NoteInfo> {
        if (limit <= 0) return emptyList()
        return toNoteInfos(
            noteDao().getRecentNotesForList(
                previewChars = LIST_BODY_PREVIEW_CHARS,
                limit = limit
            ),
            bodyPreview = true
        )
    }

    override suspend fun getRecentNotesForListByParent(
        parentNodeId: String?,
        limit: Int
    ): List<NoteInfo> {
        if (limit <= 0) return emptyList()
        return toNoteInfos(
            noteDao().getRecentNotesForListByParent(
                parentNodeId = parentNodeId,
                previewChars = LIST_BODY_PREVIEW_CHARS,
                limit = limit
            ),
            bodyPreview = true
        )
    }

    override suspend fun getNotesCount(): Int = noteDao().countAll()

    override suspend fun searchNotes(query: String): Flow<List<NoteInfo>> {
        L.d(TAG, "searchNotes: $query")
        return noteDao().searchNotesForList(query, LIST_BODY_PREVIEW_CHARS)
            .map { toNoteInfos(it, bodyPreview = true) }
    }

    override suspend fun getNoteById(id: Int): List<NoteInfo> {
        return toNoteInfos(noteDao().loadById(id))
    }

    override suspend fun getNoteForViewById(id: Int): List<NoteInfo> {
        return toNoteInfos(noteDao().loadByIdForView(id))
    }

    override suspend fun addNote(note: NoteInfo) {
        val normalized = ensureNodeIds(Converter.noteInfo2Note(note))
        val existing = noteDao().getAll()
        validateNoteBodyLength(normalized)
        validateTotalItems(existing.size + 1)
        validateFolderDirectChildrenForSingle(existing, normalized, movingId = null)
        noteDao().add(normalized)
    }

    override suspend fun addNotesBatch(
        notes: List<NoteInfo>,
        onProgress: (inserted: Int, total: Int) -> Unit
    ) {
        withContext(Dispatchers.Default) {
            val total = notes.size
            if (total == 0) {
                onProgress(0, 0)
                return@withContext
            }
            val normalized = notes.map { ensureNodeIds(Converter.noteInfo2Note(it)) }
            val existing = noteDao().getAll()
            validateTotalItems(existing.size + normalized.size)
            validateAllNoteBodies(normalized)
            validateFolderDirectChildren(existing + normalized)

            deps.runInTransaction {
                var inserted = 0
                normalized.chunked(INSERT_BATCH_SIZE).forEach { chunk ->
                    noteDao().insertAll(chunk)
                    inserted += chunk.size
                    onProgress(inserted, total)
                }
            }
        }
    }

    override suspend fun deleteNote(note: NoteInfo) {
        withContext(Dispatchers.Default) {
            if (note.itemType != ITEM_TYPE_FOLDER) {
                noteDao().delete(Converter.noteInfo2Note(note))
                return@withContext
            }

            deps.runInTransaction {
                val allNotes = noteDao().getAll()
                val root = allNotes.firstOrNull { it._id == note._id }
                if (root == null) {
                    noteDao().delete(Converter.noteInfo2Note(note))
                    return@runInTransaction
                }

                val childrenByParent = allNotes.groupBy { it.parentNodeId }
                val idsToDelete = mutableListOf<Int>()
                val stack = ArrayDeque<String>()
                stack.add(root.nodeId)

                while (stack.isNotEmpty()) {
                    val parentNodeId = stack.removeLast()
                    val children = childrenByParent[parentNodeId].orEmpty()
                    children.forEach { child ->
                        idsToDelete += child._id
                        if (child.itemType == ITEM_TYPE_FOLDER) {
                            stack.add(child.nodeId)
                        }
                    }
                }
                idsToDelete += root._id
                noteDao().deleteByIds(idsToDelete)
            }
        }
    }

    override suspend fun updateNote(note: NoteInfo) {
        val fullBodyNote = if (note.isBodyPreview && note._id > 0) {
            val persisted = noteDao().loadById(note._id).firstOrNull()
            if (persisted != null) {
                note.copy(body = persisted.body, isBodyPreview = false)
            } else {
                note.copy(isBodyPreview = false)
            }
        } else {
            note.copy(isBodyPreview = false)
        }
        val normalized = ensureNodeIds(Converter.noteInfo2Note(fullBodyNote))
        val existing = noteDao().getAll()
        validateNoteBodyLength(normalized)
        validateFolderDirectChildrenForUpdate(existing, normalized)
        noteDao().update(normalized)
    }

    override suspend fun updateNoteSensitive(id: Int, isSensitive: Boolean, updateTime: Long) {
        noteDao().updateSensitiveById(id = id, isSensitive = isSensitive, updateTime = updateTime)
    }

    override suspend fun clearAllNotes() {
        withContext(Dispatchers.Default) {
            deps.runInTransaction {
                noteDao().deleteAll()
            }
        }
    }

    override suspend fun backup(
        pwd: String,
        mode: BackupSecurityMode,
        onProgress: (completedSteps: Int, totalSteps: Int) -> Unit
    ): ByteArray {
        val resolvedMode = if (
            mode == BackupSecurityMode.DEVICE_BOUND &&
            !LocalNoteRepositoryPlatform.supportsDeviceBoundBackup()
        ) {
            L.w(TAG, "DEVICE_BOUND backup is not supported on this platform, fallback to CROSS_DEVICE")
            BackupSecurityMode.CROSS_DEVICE
        } else {
            mode
        }
        val notes = noteDao().getAll()
        val totalUnits = (notes.size + 1).coerceAtLeast(1)
        onProgress(0, totalUnits)
        val json = serializeNotesJsonWithProgress(notes) { completed, total ->
            onProgress(completed, (total + 1).coerceAtLeast(1))
        }
        val bytes = json.encodeToByteArray()
        val enc = deps.encryptor(pwd, bytes, resolvedMode)
        onProgress(totalUnits, totalUnits)
        return enc
    }

    override suspend fun restore(
        pwd: String,
        inputBytes: ByteArray,
        onProgress: (completedSteps: Int, totalSteps: Int) -> Unit
    ) {
        onProgress(0, 1)
        val bytes = inputBytes
        onProgress(1, 1)

        // 1. Decrypt
        val decryptedBytes = try {
            deps.decryptor(pwd, bytes)
        } catch (e: Exception) {
            val reason = e.message?.takeIf { it.isNotBlank() } ?: "Wrong password?"
            throw RuntimeException("Decryption failed: $reason", e)
        }
        // 2. Try parsing as JSON (New Format)
        try {
            val jsonString = decryptedBytes.decodeToString()
            var parsedTotal = 0
            val notes = normalizeNodeGraph(
                parseNotesJson(jsonString, allowLegacyCompat = true) { parsed, total ->
                    parsedTotal = total
                    val uiTotal = total.coerceAtLeast(1)
                    onProgress((parsed / 2).coerceAtMost(uiTotal), uiTotal)
                },
                regenerateNodeIds = false
            )
            validateHierarchyGraph(notes)
            validateLimitsForReplace(notes)
            val uiTotal = notes.size.coerceAtLeast(1)
            replaceNotes(notes) { inserted, total ->
                val merged =
                    ((parsedTotal.coerceAtLeast(total) + inserted) / 2).coerceAtMost(uiTotal)
                onProgress(merged, uiTotal)
            }
            onProgress(uiTotal, uiTotal)
            return
        } catch (e: JsonUtils.JsonParseException) {
            L.e(TAG, "JSON parse failed, trying legacy binary restore...", e)
        } catch (e: IllegalStateException) {
            if (e.message?.startsWith("Invalid hierarchy:") == true) {
                throw e
            }
            L.e(
                TAG,
                "JSON parse failed (structure mismatch), trying legacy binary restore...",
                e
            )
        }

        // 3. Fallback: Binary SQLite (Legacy Format)
        val notes = deps.restoreLegacyNotes(decryptedBytes, onProgress)
        if (notes.isEmpty()) {
            throw RuntimeException("Legacy database opened but contained no notes.")
        }
        val uiTotal = notes.size.coerceAtLeast(1)
        replaceNotes(notes) { inserted, total ->
            val merged = ((total + inserted) / 2).coerceAtMost(uiTotal)
            onProgress(merged, uiTotal)
        }
        onProgress(uiTotal, uiTotal)
    }

    override suspend fun exportPlain(
        onProgress: (completedSteps: Int, totalSteps: Int) -> Unit
    ): ByteArray {
        val allNotes = noteDao().getAll()
        val notes = allNotes
            .filter { !it.isSensitive }
            .filterNot { note -> isDescendantOfSensitiveFolder(note, allNotes) }
        val totalUnits = (notes.size + 1).coerceAtLeast(1)
        onProgress(0, totalUnits)
        val json = serializeNotesJsonWithProgress(notes) { completed, total ->
            onProgress(completed, (total + 1).coerceAtLeast(1))
        }
        val output = json.encodeToByteArray()
        onProgress(totalUnits, totalUnits)
        return output
    }

    private fun isDescendantOfSensitiveFolder(note: Note, allNotes: List<Note>): Boolean {
        val byNodeId = allNotes.associateBy { it.nodeId }
        var parentNodeId = note.parentNodeId
        val visited = mutableSetOf<String>()
        while (parentNodeId != null && visited.add(parentNodeId)) {
            val parent = byNodeId[parentNodeId] ?: break
            if (parent.itemType == ITEM_TYPE_FOLDER && parent.isSensitive) {
                return true
            }
            parentNodeId = parent.parentNodeId
        }
        return false
    }

    override suspend fun importPlainAsNew(
        inputBytes: ByteArray,
        onProgress: (completedSteps: Int, totalSteps: Int) -> Unit
    ) {
        onProgress(0, 1)
        val bytes = inputBytes
        onProgress(1, 1)
        val json = bytes.decodeToString()
        var parsedCount = 0
        var parsedTotal = 0
        val notes = normalizeNodeGraph(
            parseNotesJson(json, allowLegacyCompat = false) { parsed, total ->
                parsedCount = parsed
                parsedTotal = total
                val uiTotal = total.coerceAtLeast(1)
                onProgress((parsed / 2).coerceAtMost(uiTotal), uiTotal)
            },
            regenerateNodeIds = true
        )
        validateHierarchyGraph(notes)
        val existing = noteDao().getAll()
        validateLimitsForImport(existing, notes)
        val now = currentTimeMillis()
        val sortedByTime =
            notes.sortedWith(compareBy<Note> { it.updateTime }.thenBy { it.createTime }
                .thenBy { it._id })
        val baseId = noteDao().maxId() + 1
        val mappedAsNew = sortedByTime.mapIndexed { index, note ->
            val newId = baseId + index
            note.copy(
                _id = newId,
                createTime = if (note.createTime <= 0L) now else note.createTime,
                updateTime = if (note.updateTime <= 0L) now else note.updateTime
            )
        }
        val insertTotal = mappedAsNew.size
        val uiTotal = insertTotal.coerceAtLeast(1)
        deps.runInTransaction {
            var inserted = 0
            mappedAsNew.chunked(INSERT_BATCH_SIZE).forEach { chunk ->
                noteDao().insertAll(chunk)
                inserted += chunk.size
                val merged =
                    ((parsedTotal.coerceAtLeast(parsedCount) + inserted) / 2).coerceAtMost(uiTotal)
                onProgress(merged, uiTotal)
            }
        }
        onProgress(uiTotal, uiTotal)
    }

    private suspend fun replaceNotes(
        notes: List<Note>,
        onInsertProgress: ((inserted: Int, total: Int) -> Unit)? = null
    ) {
        val normalized = normalizeNodeGraph(notes, regenerateNodeIds = false)
        validateLimitsForReplace(normalized)
        deps.runInTransaction {
            noteDao().deleteAll()
            if (normalized.isEmpty()) {
                onInsertProgress?.invoke(0, 0)
            } else {
                var inserted = 0
                normalized.chunked(INSERT_BATCH_SIZE).forEach { chunk ->
                    noteDao().insertAll(chunk)
                    inserted += chunk.size
                    onInsertProgress?.invoke(inserted, normalized.size)
                }
            }
        }
    }

    private data class ParsedJsonNote(
        val note: Note,
        val legacyParentId: Int?
    )

    private fun parseNotesJson(
        jsonString: String,
        allowLegacyCompat: Boolean,
        onProgress: ((parsed: Int, total: Int) -> Unit)? = null
    ): List<Note> {
        if (!jsonString.trim().startsWith("[") && !jsonString.trim().startsWith("{")) {
            throw JsonUtils.JsonParseException("Not JSON")
        }

        val jsonArray = JsonUtils.parseJsonArrayOfObjects(jsonString)
        val totalObjects = jsonArray.size

        val parsedNotes = mutableListOf<ParsedJsonNote>()
        val dateRequest = """^\d{4}/\d{2}/\d{2}/\d{2}/\d{2}$""".toRegex()

        for (obj in jsonArray) {
            var id = 0
            var title = ""
            var body = ""
            var createTime = currentTimeMillis()
            var updateTime = currentTimeMillis()
            var color = 0
            var itemType = 0
            var nodeId = ""
            var parentNodeId: String? = null
            var legacyParentId: Int? = null
            var isSensitive = false

            JsonUtils.getInt(obj, "_id")?.let { id = it }
            JsonUtils.getString(obj, "title")?.let { title = it }
            JsonUtils.getString(obj, "body")?.let { body = it }
            JsonUtils.getLong(obj, "createTime")?.let { createTime = it }
            JsonUtils.getLong(obj, "updateTime")?.let { updateTime = it }

            if (allowLegacyCompat) {
                JsonUtils.value(obj, "date")?.let { dateElem ->
                    val legacyDate = when (dateElem) {
                        is Number -> dateElem.toLong()
                        else -> deps.parseLegacyDate(dateElem.toString())
                    }
                    if (!obj.containsKey("createTime")) createTime = legacyDate
                    if (!obj.containsKey("updateTime")) updateTime = legacyDate
                }
            }

            JsonUtils.getInt(obj, "color")?.let { color = it }
            JsonUtils.getInt(obj, "itemType")?.let { itemType = it }
            JsonUtils.getString(obj, "nodeId")?.let { nodeId = it }
            JsonUtils.getString(obj, "parentNodeId")?.let { parentNodeId = it }
            JsonUtils.getBoolean(obj, "isSensitive")?.let { isSensitive = it }

            if (allowLegacyCompat) {
                JsonUtils.getInt(obj, "parentId")?.let { legacyParentId = it }
                JsonUtils.getInt(obj, "item_type")?.let { itemType = it }
                JsonUtils.getInt(obj, "parent_id")?.let { legacyParentId = it }
                JsonUtils.getString(obj, "node_id")?.let { nodeId = it }
                JsonUtils.getString(obj, "parent_node_id")?.let { parentNodeId = it }
                JsonUtils.getBoolean(obj, "is_sensitive")?.let { isSensitive = it }
            }

            if (nodeId.isBlank()) {
                nodeId = newNodeId()
            }
            if (allowLegacyCompat && body.isEmpty()) {
                for (key in JsonUtils.keys(obj)) {
                    val value = JsonUtils.value(obj, key)
                    if (key == "_id" || key == "title" || key == "createTime" || key == "updateTime") continue
                    if (value == null) continue

                    when (value) {
                        is String -> {
                            if (value.matches(dateRequest) && !obj.containsKey("createTime")) {
                                val parsed = deps.parseLegacyDate(value)
                                createTime = parsed
                                updateTime = parsed
                            } else if (body.isEmpty()) {
                                body = value
                            }
                        }

                        is Number -> {
                            if (color == 0) {
                                color = value.toInt()
                            }
                        }
                    }
                }
            }

            parsedNotes.add(
                ParsedJsonNote(
                    note = Note(
                        _id = id,
                        nodeId = nodeId,
                        title = title,
                        body = body,
                        createTime = createTime,
                        updateTime = updateTime,
                        color = color,
                        itemType = itemType,
                        parentNodeId = parentNodeId,
                        isSensitive = isSensitive
                    ),
                    legacyParentId = legacyParentId
                )
            )
            onProgress?.invoke(parsedNotes.size, totalObjects)
        }
        if (!allowLegacyCompat) return parsedNotes.map { it.note }

        val idToNodeId = parsedNotes
            .map { it.note }
            .filter { it._id != 0 }
            .associate { it._id to it.nodeId }
        return parsedNotes.map { parsed ->
            if (!parsed.note.parentNodeId.isNullOrBlank()) {
                parsed.note
            } else {
                parsed.note.copy(parentNodeId = parsed.legacyParentId?.let { idToNodeId[it] })
            }
        }
    }

    private fun serializeNotesJsonWithProgress(
        notes: List<Note>,
        onProgress: (completed: Int, total: Int) -> Unit
    ): String {
        if (notes.isEmpty()) {
            onProgress(0, 0)
            return "[]"
        }
        val total = notes.size
        val builder = StringBuilder()
        builder.append('[')
        notes.forEachIndexed { index, note ->
            if (index > 0) builder.append(',')
            builder.append(JsonUtils.toJson(note))
            onProgress(index + 1, total)
        }
        builder.append(']')
        return builder.toString()
    }

    private fun noteDao(): NoteDao = deps.noteDaoProvider()

    private fun ensureNodeIds(note: Note): Note {
        if (note.nodeId.isNotBlank()) return note
        return note.copy(nodeId = newNodeId())
    }

    private fun validateLimitsForReplace(notes: List<Note>) {
        validateTotalItems(notes.size)
        validateAllNoteBodies(notes)
        validateFolderDirectChildren(notes)
    }

    private fun validateLimitsForImport(existing: List<Note>, incoming: List<Note>) {
        validateTotalItems(existing.size + incoming.size)
        validateAllNoteBodies(incoming)
        validateFolderDirectChildren(existing + incoming)
    }

    private fun validateTotalItems(totalItems: Int) {
        if (totalItems > MAX_TOTAL_ITEMS) {
            throw NoteLimitException(NoteLimitType.TOTAL_ITEMS, MAX_TOTAL_ITEMS)
        }
    }

    private fun validateNoteBodyLength(note: Note) {
        if (note.itemType == ITEM_TYPE_FOLDER) return
        if (note.body.length > MAX_NOTE_BODY_LENGTH) {
            throw NoteLimitException(NoteLimitType.NOTE_BODY_LENGTH, MAX_NOTE_BODY_LENGTH)
        }
    }

    private fun validateAllNoteBodies(notes: List<Note>) {
        notes.forEach(::validateNoteBodyLength)
    }

    private fun validateFolderDirectChildrenForSingle(
        existing: List<Note>,
        candidate: Note,
        movingId: Int?
    ) {
        val parentNodeId = candidate.parentNodeId ?: return
        val currentChildren = existing.count {
            it.parentNodeId == parentNodeId && (movingId == null || it._id != movingId)
        }
        if (currentChildren >= MAX_FOLDER_DIRECT_ITEMS) {
            throw NoteLimitException(NoteLimitType.FOLDER_DIRECT_ITEMS, MAX_FOLDER_DIRECT_ITEMS)
        }
    }

    private fun validateFolderDirectChildrenForUpdate(existing: List<Note>, updated: Note) {
        val current = existing.find { it._id == updated._id } ?: return
        if (current.parentNodeId == updated.parentNodeId) return
        validateFolderDirectChildrenForSingle(existing, updated, movingId = updated._id)
    }

    private fun validateFolderDirectChildren(notes: List<Note>) {
        val folderNodeIds = notes
            .asSequence()
            .filter { it.itemType == ITEM_TYPE_FOLDER }
            .map { it.nodeId }
            .toSet()
        val childCounter = mutableMapOf<String, Int>()
        for (note in notes) {
            val parentNodeId = note.parentNodeId ?: continue
            if (!folderNodeIds.contains(parentNodeId)) continue
            val next = (childCounter[parentNodeId] ?: 0) + 1
            if (next > MAX_FOLDER_DIRECT_ITEMS) {
                throw NoteLimitException(NoteLimitType.FOLDER_DIRECT_ITEMS, MAX_FOLDER_DIRECT_ITEMS)
            }
            childCounter[parentNodeId] = next
        }
    }

    private fun validateHierarchyGraph(notes: List<Note>) {
        if (notes.isEmpty()) return
        val byNodeId = notes.associateBy { it.nodeId }
        notes.forEach { note ->
            val parentNodeId = note.parentNodeId ?: return@forEach
            if (parentNodeId == note.nodeId) {
                throw IllegalStateException("Invalid hierarchy: self cycle at nodeId=${note.nodeId}")
            }
            var cursor: String? = parentNodeId
            val visiting = mutableSetOf(note.nodeId)
            while (cursor != null) {
                if (!visiting.add(cursor)) {
                    throw IllegalStateException("Invalid hierarchy: cycle detected at nodeId=$cursor")
                }
                cursor = byNodeId[cursor]?.parentNodeId
            }
        }
    }

    private fun normalizeNodeGraph(notes: List<Note>, regenerateNodeIds: Boolean): List<Note> {
        if (notes.isEmpty()) return emptyList()

        val usedNodeIds = mutableSetOf<String>()
        val sourceNodeIdToAssigned = mutableMapOf<String, String>()

        val withAssignedNodeIds = notes.map { note ->
            val sourceNodeId = note.nodeId.takeIf { it.isNotBlank() }
            val assignedNodeId = when {
                regenerateNodeIds -> uniqueNodeId(usedNodeIds)
                sourceNodeId != null && usedNodeIds.add(sourceNodeId) -> sourceNodeId
                else -> uniqueNodeId(usedNodeIds)
            }
            if (!sourceNodeId.isNullOrBlank()) {
                sourceNodeIdToAssigned[sourceNodeId] = assignedNodeId
            }
            note.copy(nodeId = assignedNodeId)
        }

        return withAssignedNodeIds.map { note ->
            val resolvedParentNodeId = when {
                !note.parentNodeId.isNullOrBlank() && regenerateNodeIds -> {
                    sourceNodeIdToAssigned[note.parentNodeId]
                }

                !note.parentNodeId.isNullOrBlank() -> {
                    note.parentNodeId
                }

                else -> null
            }
            note.copy(
                parentNodeId = resolvedParentNodeId
            )
        }
    }

    private fun toNoteInfos(notes: List<Note>, bodyPreview: Boolean = false): List<NoteInfo> {
        return notes.map { Converter.note2NoteInfo(it).copy(isBodyPreview = bodyPreview) }
    }

    private fun uniqueNodeId(used: MutableSet<String>): String {
        var id = newNodeId()
        while (!used.add(id)) {
            id = newNodeId()
        }
        return id
    }

    private fun newNodeId(): String = generateUuidString()

    data class Deps(
        val noteDaoProvider: () -> NoteDao,
        val runInTransaction: (suspend () -> Unit) -> Unit,
        val encryptor: (String, ByteArray, BackupSecurityMode) -> ByteArray,
        val decryptor: (String, ByteArray) -> ByteArray,
        val parseLegacyDate: (String) -> Long,
        val restoreLegacyNotes: suspend (
            decryptedBytes: ByteArray,
            onProgress: (completedSteps: Int, totalSteps: Int) -> Unit
        ) -> List<Note>
    ) {
        companion object {
            fun default(): Deps = Deps(
                noteDaoProvider = LocalNoteRepositoryPlatform::noteDao,
                runInTransaction = LocalNoteRepositoryPlatform::runInTransaction,
                encryptor = LocalNoteRepositoryPlatform::encryptBackup,
                decryptor = LocalNoteRepositoryPlatform::decryptBackup,
                parseLegacyDate = LocalNoteRepositoryPlatform::parseLegacyDate,
                restoreLegacyNotes = LocalNoteRepositoryPlatform::restoreLegacyNotes
            )
        }
    }

    companion object {

        const val TAG = "LocalNoteRepositoryImpl"
        private const val LIST_BODY_PREVIEW_CHARS = 512
        private const val INSERT_BATCH_SIZE = 200
    }

}
