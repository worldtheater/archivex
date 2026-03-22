package com.worldtheater.archive.domain.repository

import com.worldtheater.archive.domain.AppTheme
import com.worldtheater.archive.domain.BackupSecurityMode
import com.worldtheater.archive.domain.BiometricReAuthInterval
import com.worldtheater.archive.domain.SensitiveNoteAuthFrequency
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val themeFlow: StateFlow<AppTheme>
    val biometricFlow: StateFlow<Boolean>
    val biometricReAuthIntervalFlow: StateFlow<BiometricReAuthInterval>
    val sensitiveNoteAuthFrequencyFlow: StateFlow<SensitiveNoteAuthFrequency>
    val editorAutoSaveEnabledFlow: StateFlow<Boolean>
    val allowScreenshotsFlow: StateFlow<Boolean>
    val backupDirFlow: StateFlow<String?>
    val backupSecurityModeFlow: StateFlow<BackupSecurityMode>
    val lastBackupFilenameFlow: StateFlow<String?>
    val lastBackupTimeFlow: StateFlow<Long>
    val hasBackupPasswordFlow: StateFlow<Boolean>

    suspend fun setBiometricDisabled(disabled: Boolean)
    suspend fun setBiometricReauthInterval(interval: BiometricReAuthInterval)
    suspend fun setSensitiveNoteAuthFrequency(frequency: SensitiveNoteAuthFrequency)
    suspend fun setEditorAutoSaveEnabled(enabled: Boolean)
    suspend fun setAllowScreenshots(enabled: Boolean)
    suspend fun setAppTheme(theme: AppTheme)
    suspend fun setBackupDir(uriString: String)
    suspend fun setBackupSecurityMode(mode: BackupSecurityMode)
    suspend fun setLastBackupInfo(filename: String, time: Long)
    suspend fun updateBackupPasswordState()
    suspend fun isGuidePresetDataSeeded(): Boolean
    suspend fun setGuidePresetDataSeeded(seeded: Boolean)

    fun generateBackupFilename(): String

    fun isBiometricDisabled(): Boolean
    fun getBiometricReauthInterval(): BiometricReAuthInterval
    fun getSensitiveNoteAuthFrequency(): SensitiveNoteAuthFrequency
    fun isEditorAutoSaveEnabled(): Boolean
    fun isAllowScreenshotsEnabled(): Boolean
    fun getAppTheme(): AppTheme
    fun getBackupDir(): String?
    fun getBackupSecurityMode(): BackupSecurityMode
}
