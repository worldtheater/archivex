package com.worldtheater.archive.platform.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.worldtheater.archive.AppContextHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

actual object SettingsKvStore {
    private val context get() = AppContextHolder.appContext
    private val Context.settingsDataStore by preferencesDataStore(name = "archive_app_settings")

    actual fun stringFlow(key: String, defaultValue: String?): Flow<String?> {
        return context.settingsDataStore.data.map { prefs ->
            prefs.coerceString(key) ?: defaultValue
        }
    }

    actual suspend fun putString(key: String, value: String?) {
        val prefKey = stringPreferencesKey(key)
        context.settingsDataStore.edit { prefs ->
            prefs.removeLegacyTypedKeys(key)
            if (value == null) {
                prefs.remove(prefKey)
            } else {
                prefs[prefKey] = value
            }
        }
    }

    actual suspend fun getString(key: String): String? {
        return context.settingsDataStore.data.first().coerceString(key)
    }

    actual suspend fun clearLegacyBackupPasswordFile() {
        val legacy = context.filesDir?.resolve(LEGACY_BACKUP_PWD_FILE)
        if (legacy?.exists() == true) {
            legacy.delete()
        }
    }

    actual fun generateBackupFilename(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "archive_app_backup_${timestamp}.enc"
    }

    private fun Preferences.coerceString(key: String): String? {
        return runCatching {
            this[stringPreferencesKey(key)]
                ?: this[intPreferencesKey(key)]?.toString()
                ?: this[longPreferencesKey(key)]?.toString()
                ?: this[booleanPreferencesKey(key)]?.toString()
                ?: this[floatPreferencesKey(key)]?.toString()
                ?: this[doublePreferencesKey(key)]?.toString()
        }.getOrNull()
    }

    private fun MutablePreferences.removeLegacyTypedKeys(key: String) {
        remove(intPreferencesKey(key))
        remove(longPreferencesKey(key))
        remove(booleanPreferencesKey(key))
        remove(floatPreferencesKey(key))
        remove(doublePreferencesKey(key))
    }

    private const val LEGACY_BACKUP_PWD_FILE = "bk_pwd"
}
