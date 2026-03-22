package com.worldtheater.archive.platform.data

import com.worldtheater.archive.data.local.Note
import com.worldtheater.archive.data.local.NoteDao
import com.worldtheater.archive.domain.BackupSecurityMode

expect object LocalNoteRepositoryPlatform {
    fun supportsDeviceBoundBackup(): Boolean
    fun ensureDatabaseInitialized()
    fun noteDao(): NoteDao
    fun runInTransaction(block: suspend () -> Unit)
    fun encryptBackup(alias: String, input: ByteArray, mode: BackupSecurityMode): ByteArray
    fun decryptBackup(alias: String, input: ByteArray): ByteArray
    fun parseLegacyDate(dateString: String): Long
    suspend fun restoreLegacyNotes(
        decryptedBytes: ByteArray,
        onProgress: (completedSteps: Int, totalSteps: Int) -> Unit
    ): List<Note>
}
