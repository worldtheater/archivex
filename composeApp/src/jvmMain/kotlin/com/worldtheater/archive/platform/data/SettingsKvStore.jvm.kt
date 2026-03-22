package com.worldtheater.archive.platform.data

import com.worldtheater.archive.platform.system.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.Properties

actual object SettingsKvStore {
    private val storageFile: File by lazy {
        File(
            System.getProperty("user.home"),
            ".archivex/settings.properties"
        )
    }
    private val state = MutableStateFlow(load())

    actual fun stringFlow(key: String, defaultValue: String?): Flow<String?> {
        return state.map { it[key] ?: defaultValue }
    }

    actual suspend fun putString(key: String, value: String?) {
        val updated = if (value == null) {
            state.value - key
        } else {
            state.value + (key to value)
        }
        state.value = updated
        persist(updated)
    }

    actual suspend fun getString(key: String): String? = state.value[key]

    actual suspend fun clearLegacyBackupPasswordFile() = Unit

    actual fun generateBackupFilename(): String {
        return "archive_app_backup_${currentTimeMillis()}.enc"
    }

    private fun load(): Map<String, String> {
        if (!storageFile.exists()) return emptyMap()
        return runCatching {
            val properties = Properties()
            storageFile.inputStream().use { input -> properties.load(input) }
            properties.stringPropertyNames().associateWith { key ->
                properties.getProperty(key)
            }
        }.getOrDefault(emptyMap())
    }

    private fun persist(values: Map<String, String>) {
        runCatching {
            storageFile.parentFile?.mkdirs()
            val properties = Properties()
            values.forEach { (key, value) -> properties.setProperty(key, value) }
            storageFile.outputStream().use { output ->
                properties.store(output, "Archive desktop settings")
            }
        }
    }
}
