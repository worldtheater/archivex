package com.worldtheater.archive.platform.data

import androidx.room.Room
import androidx.room.Transactor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.worldtheater.archive.data.local.AppDataBase
import com.worldtheater.archive.data.local.AppDataBaseConstructor
import com.worldtheater.archive.data.local.Note
import com.worldtheater.archive.data.local.NoteDao
import com.worldtheater.archive.platform.system.currentTimeMillis
import com.worldtheater.archive.domain.BackupSecurityMode
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSHomeDirectory

actual object LocalNoteRepositoryPlatform {
    actual fun supportsDeviceBoundBackup(): Boolean = false

    actual fun ensureDatabaseInitialized() {
        initializeDb()
    }

    actual fun noteDao(): NoteDao {
        initializeDb()
        return AppDataBase.db.noteDao()
    }

    actual fun runInTransaction(block: suspend () -> Unit) {
        initializeDb()
        runBlocking {
            AppDataBase.db.useConnection(isReadOnly = false) { transactor ->
                transactor.withTransaction(Transactor.SQLiteTransactionType.IMMEDIATE) {
                    block()
                }
            }
            AppDataBase.db.invalidationTracker.refreshAsync()
        }
    }

    actual fun encryptBackup(alias: String, input: ByteArray, mode: BackupSecurityMode): ByteArray {
        throw IllegalStateException("Backup encryption is not implemented for iOS yet")
    }

    actual fun decryptBackup(alias: String, input: ByteArray): ByteArray {
        throw IllegalStateException("Backup decryption is not implemented for iOS yet")
    }

    actual fun parseLegacyDate(dateString: String): Long = currentTimeMillis()

    actual suspend fun restoreLegacyNotes(
        decryptedBytes: ByteArray,
        onProgress: (completedSteps: Int, totalSteps: Int) -> Unit
    ): List<Note> = emptyList()

    private fun initializeDb() {
        val dbPath = "${NSHomeDirectory()}/Library/$DATABASE_NAME"
        AppDataBase.init(
            Room.databaseBuilder<AppDataBase>(
                name = dbPath,
                factory = { AppDataBaseConstructor.initialize() }
            ).setDriver(BundledSQLiteDriver())
        )
    }

    private const val DATABASE_NAME = "main.db"
}
