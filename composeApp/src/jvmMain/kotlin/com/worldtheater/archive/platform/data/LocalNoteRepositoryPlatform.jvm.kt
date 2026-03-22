package com.worldtheater.archive.platform.data

import androidx.room.Room
import androidx.room.Transactor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.worldtheater.archive.data.local.AppDataBase
import com.worldtheater.archive.data.local.AppDataBase_Impl
import com.worldtheater.archive.data.local.Note
import com.worldtheater.archive.data.local.NoteDao
import com.worldtheater.archive.platform.security.JvmBackupSecret
import com.worldtheater.archive.platform.system.currentTimeMillis
import com.worldtheater.archive.domain.BackupSecurityMode
import kotlinx.coroutines.runBlocking
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

actual object LocalNoteRepositoryPlatform {
    actual fun supportsDeviceBoundBackup(): Boolean = false

    actual fun ensureDatabaseInitialized() {
        requireDb()
    }

    actual fun noteDao(): NoteDao {
        return requireDb().noteDao()
    }

    actual fun runInTransaction(block: suspend () -> Unit) {
        runBlocking {
            requireDb().useConnection(isReadOnly = false) { transactor ->
                transactor.withTransaction(Transactor.SQLiteTransactionType.IMMEDIATE) {
                    block()
                }
            }
        }
        requireDb().invalidationTracker.refreshAsync()
    }

    actual fun encryptBackup(alias: String, input: ByteArray, mode: BackupSecurityMode): ByteArray {
        return when (mode) {
            BackupSecurityMode.CROSS_DEVICE -> JvmBackupSecret.encrypt(alias, input)
            BackupSecurityMode.DEVICE_BOUND -> JvmBackupSecret.encrypt(alias, input)
        }
    }

    actual fun decryptBackup(alias: String, input: ByteArray): ByteArray {
        return JvmBackupSecret.decrypt(alias, input)
    }

    actual fun parseLegacyDate(dateString: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy/MM/dd/HH/mm", Locale.getDefault())
            format.parse(dateString)?.time ?: currentTimeMillis()
        } catch (_: Exception) {
            currentTimeMillis()
        }
    }

    actual suspend fun restoreLegacyNotes(
        decryptedBytes: ByteArray,
        onProgress: (completedSteps: Int, totalSteps: Int) -> Unit
    ): List<Note> {
        throw IllegalStateException(
            "Legacy binary backup restore is not supported on JVM desktop yet"
        )
    }

    private fun requireDb(): AppDataBase {
        return try {
            AppDataBase.db
        } catch (_: UninitializedPropertyAccessException) {
            initializeDb()
            AppDataBase.db
        }
    }

    private fun initializeDb() {
        val dbFile = File(File(System.getProperty("user.home"), ".archivex"), DATABASE_NAME)
        dbFile.parentFile?.mkdirs()
        AppDataBase.init(
            Room.databaseBuilder<AppDataBase>(
                name = dbFile.absolutePath,
                factory = { AppDataBase_Impl() }
            ).setDriver(BundledSQLiteDriver())
        )
    }

    private const val DATABASE_NAME = "main.db"
}
