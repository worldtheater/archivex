package com.worldtheater.archive.domain.repository

import com.worldtheater.archive.domain.BackupSecurityMode
import com.worldtheater.archive.domain.model.NoteInfo
import kotlinx.coroutines.flow.Flow

interface NoteRepository {

    suspend fun getAllNotes(): List<NoteInfo>

    suspend fun allNotes(): Flow<List<NoteInfo>>

    suspend fun getRecentNotesForList(limit: Int): List<NoteInfo> {
        if (limit <= 0) return emptyList()
        return getAllNotes().take(limit)
    }

    suspend fun getRecentNotesForListByParent(parentNodeId: String?, limit: Int): List<NoteInfo> {
        if (limit <= 0) return emptyList()
        return getRecentNotesForList(limit).filter { it.parentNodeId == parentNodeId }
    }

    suspend fun getNotesCount(): Int = getAllNotes().size

    suspend fun searchNotes(query: String): Flow<List<NoteInfo>>

    suspend fun getNoteById(id: Int): List<NoteInfo>

    suspend fun getNoteForViewById(id: Int): List<NoteInfo> {
        return getNoteById(id).map { note ->
            if (note.isSensitive) note.copy(body = "") else note
        }
    }

    suspend fun addNote(note: NoteInfo)

    suspend fun addNotesBatch(
        notes: List<NoteInfo>,
        onProgress: (inserted: Int, total: Int) -> Unit = { _, _ -> }
    ) {
        val total = notes.size
        if (total == 0) {
            onProgress(0, 0)
            return
        }
        var inserted = 0
        notes.forEach { note ->
            addNote(note)
            inserted += 1
            onProgress(inserted, total)
        }
    }

    suspend fun deleteNote(note: NoteInfo)

    suspend fun updateNote(note: NoteInfo)

    suspend fun updateNoteSensitive(id: Int, isSensitive: Boolean, updateTime: Long)

    suspend fun clearAllNotes() {
        getAllNotes().forEach { note ->
            deleteNote(note)
        }
    }

    suspend fun backup(
        pwd: String,
        mode: BackupSecurityMode,
        onProgress: (completedSteps: Int, totalSteps: Int) -> Unit = { _, _ -> }
    ): ByteArray

    suspend fun restore(
        pwd: String,
        inputBytes: ByteArray,
        onProgress: (completedSteps: Int, totalSteps: Int) -> Unit = { _, _ -> }
    )

    suspend fun exportPlain(
        onProgress: (completedSteps: Int, totalSteps: Int) -> Unit = { _, _ -> }
    ): ByteArray

    suspend fun importPlainAsNew(
        inputBytes: ByteArray,
        onProgress: (completedSteps: Int, totalSteps: Int) -> Unit = { _, _ -> }
    )
}
