package com.worldtheater.archive.platform.data

import kotlinx.coroutines.flow.Flow

expect object SettingsKvStore {
    fun stringFlow(key: String, defaultValue: String? = null): Flow<String?>
    suspend fun putString(key: String, value: String?)
    suspend fun getString(key: String): String?
    suspend fun clearLegacyBackupPasswordFile()
    fun generateBackupFilename(): String
}
