package com.worldtheater.archive.platform.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.worldtheater.archive.AppContextHolder
import com.worldtheater.archive.data.local.AppDataBase
import com.worldtheater.archive.data.local.Note
import com.worldtheater.archive.data.local.NoteDao
import com.worldtheater.archive.domain.BackupSecurityMode
import com.worldtheater.archive.platform.security.Secret
import com.worldtheater.archive.util.TimeUtils
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

actual object LocalNoteRepositoryPlatform {
    actual fun supportsDeviceBoundBackup(): Boolean = true

    actual fun ensureDatabaseInitialized() {
        requireDb()
    }

    actual fun noteDao(): NoteDao {
        return requireDb().noteDao()
    }

    actual fun runInTransaction(block: suspend () -> Unit) {
        requireDb().runInTransaction {
            runBlocking { block() }
        }
    }

    actual fun encryptBackup(alias: String, input: ByteArray, mode: BackupSecurityMode): ByteArray {
        return Secret.encryptBackup(alias, input, mode)
    }

    actual fun decryptBackup(alias: String, input: ByteArray): ByteArray {
        return Secret.decrypt(alias, input)
    }

    actual fun parseLegacyDate(dateString: String): Long = TimeUtils.parseLegacyDate(dateString)

    actual suspend fun restoreLegacyNotes(
        decryptedBytes: ByteArray,
        onProgress: (completedSteps: Int, totalSteps: Int) -> Unit
    ): List<Note> {
        onProgress(0, 1)
        val rows = readLegacyRows(decryptedBytes)
        return restoreNotesFromLegacyRows(
            rows = rows,
            parseLegacyDate = ::parseLegacyDate,
            onProgress = onProgress
        )
    }

    private fun readLegacyRows(decryptedBytes: ByteArray): List<LegacyNoteRow> {
        val dbPath = requireNotNull(AppDataBase.db.openHelper.writableDatabase.path) {
            "Database path is unavailable"
        }
        val parentDir = requireNotNull(File(dbPath).parentFile) {
            "Database parent directory is unavailable"
        }
        val tempFile = File(parentDir, "temp_legacy_restore.db")

        return try {
            tempFile.writeBytes(decryptedBytes)
            val sqlite = SQLiteDatabase.openDatabase(
                tempFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY
            )

            val rows = mutableListOf<LegacyNoteRow>()
            sqlite.use { db ->
                db.rawQuery("SELECT * FROM notes", null).use { c ->
                    val bodyIdx = c.getColumnIndex("body")
                    val dateIdx = c.getColumnIndex("date")
                    val colorIdx = c.getColumnIndex("color")
                    while (c.moveToNext()) {
                        val body = if (bodyIdx >= 0) c.getString(bodyIdx) else ""
                        val dateStr = if (dateIdx >= 0) c.getString(dateIdx) else ""
                        val color = if (colorIdx >= 0) c.getInt(colorIdx) else 0
                        rows += LegacyNoteRow(
                            body = body,
                            legacyDateText = dateStr,
                            color = color
                        )
                    }
                }
            }
            rows
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private fun requireDb(): AppDataBase {
        return try {
            AppDataBase.db
        } catch (_: UninitializedPropertyAccessException) {
            System.loadLibrary("sqlcipher")
            val passphrase = com.worldtheater.archive.platform.security.KeyManager.getOrCreateKey()
            val factory = SupportOpenHelperFactory(passphrase)
            AppDataBase.init(
                Room.databaseBuilder(
                    AppContextHolder.appContext,
                    AppDataBase::class.java,
                    DATABASE_NAME
                ).openHelperFactory(factory)
            )
            AppDataBase.db
        }
    }

    private const val DATABASE_NAME = "main"
}
