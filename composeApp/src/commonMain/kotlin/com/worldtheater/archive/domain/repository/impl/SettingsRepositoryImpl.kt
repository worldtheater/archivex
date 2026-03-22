package com.worldtheater.archive.domain.repository.impl

import com.worldtheater.archive.domain.AppTheme
import com.worldtheater.archive.domain.BackupSecurityMode
import com.worldtheater.archive.domain.BiometricReAuthInterval
import com.worldtheater.archive.domain.SensitiveNoteAuthFrequency
import com.worldtheater.archive.domain.repository.SettingsRepository
import com.worldtheater.archive.platform.data.SettingsKvStore
import com.worldtheater.archive.util.log.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsRepositoryImpl(
    private val deps: Deps = Deps.default()
) : SettingsRepository {
    private var legacyBackupPasswordCleared = false

    private val _themeFlow = MutableStateFlow(AppTheme.SYSTEM)
    override val themeFlow: StateFlow<AppTheme> = _themeFlow

    private val _biometricFlow = MutableStateFlow(false)
    override val biometricFlow: StateFlow<Boolean> = _biometricFlow

    private val _biometricReAuthIntervalFlow =
        MutableStateFlow(BiometricReAuthInterval.ONE_MINUTE)
    override val biometricReAuthIntervalFlow: StateFlow<BiometricReAuthInterval> =
        _biometricReAuthIntervalFlow
    private val _sensitiveNoteAuthFrequencyFlow =
        MutableStateFlow(SensitiveNoteAuthFrequency.EVERY_TIME)
    override val sensitiveNoteAuthFrequencyFlow: StateFlow<SensitiveNoteAuthFrequency> =
        _sensitiveNoteAuthFrequencyFlow
    private val _editorAutoSaveEnabledFlow = MutableStateFlow(true)
    override val editorAutoSaveEnabledFlow: StateFlow<Boolean> = _editorAutoSaveEnabledFlow
    private val _allowScreenshotsFlow = MutableStateFlow(false)
    override val allowScreenshotsFlow: StateFlow<Boolean> = _allowScreenshotsFlow

    private var _cachedBiometric = false

    private val _backupDirFlow = MutableStateFlow<String?>(null)
    override val backupDirFlow: StateFlow<String?> = _backupDirFlow

    private val _backupSecurityModeFlow = MutableStateFlow(BackupSecurityMode.DEVICE_BOUND)
    override val backupSecurityModeFlow: StateFlow<BackupSecurityMode> = _backupSecurityModeFlow

    private val _lastBackupFilenameFlow = MutableStateFlow<String?>(null)
    override val lastBackupFilenameFlow: StateFlow<String?> = _lastBackupFilenameFlow

    private val _lastBackupTimeFlow = MutableStateFlow(0L)
    override val lastBackupTimeFlow: StateFlow<Long> = _lastBackupTimeFlow

    private val _hasBackupPasswordFlow = MutableStateFlow(false)
    override val hasBackupPasswordFlow: StateFlow<Boolean> = _hasBackupPasswordFlow

    init {
        observeSettings()
    }

    private fun observeSettings() {
        deps.observeScope.launch {
            deps.stringFlow(Keys.DISABLE_BIOMETRIC, "false").collect { value ->
                val parsed = value?.toBooleanStrictOrNull() ?: false
                _cachedBiometric = parsed
                _biometricFlow.value = parsed
                maybeClearLegacyBackupPasswordFile()
            }
        }
        deps.observeScope.launch {
            deps.stringFlow(Keys.BIOMETRIC_REAUTH_INTERVAL, BiometricReAuthInterval.ONE_MINUTE.ordinal.toString())
                .collect { value ->
                    val ordinal =
                        value?.toIntOrNull() ?: BiometricReAuthInterval.ONE_MINUTE.ordinal
                    _biometricReAuthIntervalFlow.value =
                        BiometricReAuthInterval.entries.toTypedArray()
                            .getOrElse(ordinal) { BiometricReAuthInterval.ONE_MINUTE }
                }
        }
        deps.observeScope.launch {
            deps.stringFlow(
                Keys.SENSITIVE_NOTE_AUTH_FREQUENCY,
                SensitiveNoteAuthFrequency.EVERY_TIME.ordinal.toString()
            ).collect { value ->
                val ordinal =
                    value?.toIntOrNull() ?: SensitiveNoteAuthFrequency.EVERY_TIME.ordinal
                _sensitiveNoteAuthFrequencyFlow.value =
                    SensitiveNoteAuthFrequency.entries.toTypedArray()
                        .getOrElse(ordinal) { SensitiveNoteAuthFrequency.EVERY_TIME }
            }
        }
        deps.observeScope.launch {
            deps.stringFlow(Keys.EDITOR_AUTO_SAVE_ENABLED, "true").collect { value ->
                _editorAutoSaveEnabledFlow.value = value?.toBooleanStrictOrNull() ?: true
            }
        }
        deps.observeScope.launch {
            deps.stringFlow(Keys.ALLOW_SCREENSHOTS, "false").collect { value ->
                _allowScreenshotsFlow.value = value?.toBooleanStrictOrNull() ?: false
            }
        }
        deps.observeScope.launch {
            deps.stringFlow(Keys.APP_THEME, AppTheme.SYSTEM.ordinal.toString()).collect { value ->
                val ordinal = value?.toIntOrNull() ?: AppTheme.SYSTEM.ordinal
                _themeFlow.value = AppTheme.entries.toTypedArray()
                    .getOrElse(ordinal) { AppTheme.SYSTEM }
            }
        }
        deps.observeScope.launch {
            deps.stringFlow(Keys.BACKUP_DIR, null).collect { value ->
                _backupDirFlow.value = value
            }
        }
        deps.observeScope.launch {
            deps.stringFlow(Keys.BACKUP_SECURITY_MODE, BackupSecurityMode.DEVICE_BOUND.ordinal.toString())
                .collect { value ->
                    val ordinal = value?.toIntOrNull() ?: BackupSecurityMode.DEVICE_BOUND.ordinal
                    _backupSecurityModeFlow.value = BackupSecurityMode.entries.toTypedArray()
                        .getOrElse(ordinal) { BackupSecurityMode.DEVICE_BOUND }
                }
        }
        deps.observeScope.launch {
            deps.stringFlow(Keys.LAST_BACKUP_FILENAME, null).collect { value ->
                _lastBackupFilenameFlow.value = value
            }
        }
        deps.observeScope.launch {
            deps.stringFlow(Keys.LAST_BACKUP_TIME, "0").collect { value ->
                _lastBackupTimeFlow.value = value?.toLongOrNull() ?: 0L
            }
        }
        deps.observeScope.launch {
            deps.stringFlow(Keys.HAS_BACKUP_PASSWORD, "false").collect { value ->
                _hasBackupPasswordFlow.value = value?.toBooleanStrictOrNull() ?: false
            }
        }
    }

    private suspend fun maybeClearLegacyBackupPasswordFile() {
        if (legacyBackupPasswordCleared) return
        legacyBackupPasswordCleared = true
        runCatching { deps.clearLegacyBackupPasswordFile() }
            .onFailure { e ->
                L.e(TAG, "Failed to clear legacy backup password file", e)
            }
    }

    override suspend fun setBiometricDisabled(disabled: Boolean) {
        L.d(TAG, "setBiometricDisabled: $disabled")
        deps.putString(Keys.DISABLE_BIOMETRIC, disabled.toString())
    }

    override suspend fun setBiometricReauthInterval(interval: BiometricReAuthInterval) {
        L.d(TAG, "setBiometricReauthInterval: $interval")
        deps.putString(Keys.BIOMETRIC_REAUTH_INTERVAL, interval.ordinal.toString())
    }

    override suspend fun setSensitiveNoteAuthFrequency(frequency: SensitiveNoteAuthFrequency) {
        L.d(TAG, "setSensitiveNoteAuthFrequency: $frequency")
        deps.putString(Keys.SENSITIVE_NOTE_AUTH_FREQUENCY, frequency.ordinal.toString())
    }

    override suspend fun setEditorAutoSaveEnabled(enabled: Boolean) {
        L.d(TAG, "setEditorAutoSaveEnabled: $enabled")
        deps.putString(Keys.EDITOR_AUTO_SAVE_ENABLED, enabled.toString())
    }

    override suspend fun setAllowScreenshots(enabled: Boolean) {
        L.d(TAG, "setAllowScreenshots: $enabled")
        deps.putString(Keys.ALLOW_SCREENSHOTS, enabled.toString())
    }

    override suspend fun setAppTheme(theme: AppTheme) {
        L.d(TAG, "setAppTheme: $theme")
        deps.putString(Keys.APP_THEME, theme.ordinal.toString())
    }

    override suspend fun setBackupDir(uriString: String) {
        L.d(TAG, "setBackupDir: $uriString")
        deps.putString(Keys.BACKUP_DIR, uriString)
    }

    override suspend fun setBackupSecurityMode(mode: BackupSecurityMode) {
        deps.putString(Keys.BACKUP_SECURITY_MODE, mode.ordinal.toString())
    }

    override suspend fun setLastBackupInfo(filename: String, time: Long) {
        L.d(TAG, "setLastBackupInfo: $filename at $time")
        deps.putString(Keys.HAS_BACKUP_PASSWORD, false.toString())
        deps.putString(Keys.LAST_BACKUP_FILENAME, filename)
        deps.putString(Keys.LAST_BACKUP_TIME, time.toString())
    }

    override suspend fun updateBackupPasswordState() {
        L.d(TAG, "updateBackupPasswordState")
        deps.putString(Keys.HAS_BACKUP_PASSWORD, false.toString())
    }

    override suspend fun isGuidePresetDataSeeded(): Boolean {
        return deps.getString(Keys.GUIDE_PRESET_DATA_SEEDED)?.toBooleanStrictOrNull() == true
    }

    override suspend fun setGuidePresetDataSeeded(seeded: Boolean) {
        deps.putString(Keys.GUIDE_PRESET_DATA_SEEDED, seeded.toString())
    }

    override fun generateBackupFilename(): String = deps.generateBackupFilename()

    override fun isBiometricDisabled(): Boolean = _cachedBiometric
    override fun getBiometricReauthInterval(): BiometricReAuthInterval =
        _biometricReAuthIntervalFlow.value

    override fun getSensitiveNoteAuthFrequency(): SensitiveNoteAuthFrequency =
        _sensitiveNoteAuthFrequencyFlow.value

    override fun isEditorAutoSaveEnabled(): Boolean = _editorAutoSaveEnabledFlow.value
    override fun isAllowScreenshotsEnabled(): Boolean = _allowScreenshotsFlow.value
    override fun getAppTheme(): AppTheme = _themeFlow.value
    override fun getBackupDir(): String? = _backupDirFlow.value
    override fun getBackupSecurityMode(): BackupSecurityMode = _backupSecurityModeFlow.value

    data class Deps(
        val observeScope: CoroutineScope,
        val stringFlow: (key: String, defaultValue: String?) -> Flow<String?>,
        val putString: suspend (key: String, value: String?) -> Unit,
        val getString: suspend (key: String) -> String?,
        val clearLegacyBackupPasswordFile: suspend () -> Unit,
        val generateBackupFilename: () -> String
    ) {
        companion object {
            fun default(): Deps = Deps(
                observeScope = CoroutineScope(Dispatchers.Default),
                stringFlow = SettingsKvStore::stringFlow,
                putString = SettingsKvStore::putString,
                getString = SettingsKvStore::getString,
                clearLegacyBackupPasswordFile = SettingsKvStore::clearLegacyBackupPasswordFile,
                generateBackupFilename = SettingsKvStore::generateBackupFilename
            )
        }
    }

    private object Keys {
        const val DISABLE_BIOMETRIC = "disable_biometric"
        const val BIOMETRIC_REAUTH_INTERVAL = "biometric_reauth_interval"
        const val SENSITIVE_NOTE_AUTH_FREQUENCY = "sensitive_note_auth_frequency"
        const val EDITOR_AUTO_SAVE_ENABLED = "editor_auto_save_enabled"
        const val ALLOW_SCREENSHOTS = "allow_screenshots"
        const val APP_THEME = "app_theme"
        const val BACKUP_DIR = "backup_dir"
        const val BACKUP_SECURITY_MODE = "backup_security_mode"
        const val LAST_BACKUP_FILENAME = "last_backup_filename"
        const val LAST_BACKUP_TIME = "last_backup_time"
        const val HAS_BACKUP_PASSWORD = "has_backup_password"
        const val GUIDE_PRESET_DATA_SEEDED = "guide_preset_data_seeded"
    }

    companion object {
        private const val TAG = "SettingsRepository"
    }
}
