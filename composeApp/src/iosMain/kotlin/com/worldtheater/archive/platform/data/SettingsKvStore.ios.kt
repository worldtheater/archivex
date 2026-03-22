package com.worldtheater.archive.platform.data

import com.worldtheater.archive.platform.system.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import platform.Foundation.NSUserDefaults

actual object SettingsKvStore {
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults()
    private val state = MutableStateFlow<Map<String, String?>>(emptyMap())

    actual fun stringFlow(key: String, defaultValue: String?): Flow<String?> {
        return state.map { cache ->
            cache[key] ?: defaults.stringForKey(key) ?: defaultValue
        }
    }

    actual suspend fun putString(key: String, value: String?) {
        if (value == null) {
            defaults.removeObjectForKey(key)
        } else {
            defaults.setObject(value, forKey = key)
        }
        state.value = if (value == null) {
            state.value - key
        } else {
            state.value + (key to value)
        }
    }

    actual suspend fun getString(key: String): String? = defaults.stringForKey(key)

    actual suspend fun clearLegacyBackupPasswordFile() = Unit

    actual fun generateBackupFilename(): String {
        return "archive_app_backup_${currentTimeMillis()}.enc"
    }
}
