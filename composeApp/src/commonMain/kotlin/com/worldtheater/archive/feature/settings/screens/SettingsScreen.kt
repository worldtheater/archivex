package com.worldtheater.archive.feature.settings.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import archivex.composeapp.generated.resources.*
import com.worldtheater.archive.domain.AppTheme
import com.worldtheater.archive.domain.BiometricReAuthInterval
import com.worldtheater.archive.domain.SensitiveNoteAuthFrequency
import com.worldtheater.archive.domain.repository.NoteRepository
import com.worldtheater.archive.domain.repository.SettingsRepository
import com.worldtheater.archive.feature.note_list.screens.markdown_preview.clearMermaidSnapshotDiskCache
import com.worldtheater.archive.feature.note_list.screens.markdown_preview.rememberPlatformMermaidSnapshotCacheIo
import com.worldtheater.archive.feature.settings.components.ClearDataConfirmDialog
import com.worldtheater.archive.feature.settings.components.SettingsHapticSwitch
import com.worldtheater.archive.feature.settings.dialogs.BiometricIntervalSelectionDialog
import com.worldtheater.archive.feature.settings.dialogs.SensitiveNoteAuthFrequencyDialog
import com.worldtheater.archive.feature.settings.dialogs.ThemeSelectionDialog
import com.worldtheater.archive.platform.auth.SensitiveAuthAvailabilityProvider
import com.worldtheater.archive.platform.auth.SensitiveAuthPrompt
import com.worldtheater.archive.platform.system.UserMessageSink
import com.worldtheater.archive.ui.theme.rememberContentTopPadding
import com.worldtheater.archive.ui.widget.AppDialog
import com.worldtheater.archive.ui.widget.SettingsAppTopBar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onBackupSettingsClick: () -> Unit,
    onImportExportSettingsClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    val settingsRepository: SettingsRepository = koinInject()
    val noteRepository: NoteRepository = koinInject()
    val sensitiveAuthAvailabilityProvider: SensitiveAuthAvailabilityProvider = koinInject()
    val sensitiveAuthPrompt: SensitiveAuthPrompt = koinInject()
    val userMessageSink: UserMessageSink = koinInject()

    val isBiometricDisabled by settingsRepository.biometricFlow.collectAsState()
    val isBiometricEnabled = !isBiometricDisabled
    val biometricReauthInterval by settingsRepository.biometricReAuthIntervalFlow.collectAsState()
    val sensitiveNoteAuthFrequency by settingsRepository.sensitiveNoteAuthFrequencyFlow.collectAsState()
    val editorAutoSaveEnabled by settingsRepository.editorAutoSaveEnabledFlow.collectAsState()
    val allowScreenshots by settingsRepository.allowScreenshotsFlow.collectAsState()
    val lastBackupFilename by settingsRepository.lastBackupFilenameFlow.collectAsState()
    val lastBackupTime by settingsRepository.lastBackupTimeFlow.collectAsState()
    val currentTheme by settingsRepository.themeFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val mermaidCacheIo = rememberPlatformMermaidSnapshotCacheIo()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showBiometricIntervalDialog by remember { mutableStateOf(false) }
    var showSensitiveNoteAuthDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var isClearDataAuthenticating by remember { mutableStateOf(false) }
    var isClearingData by remember { mutableStateOf(false) }

    val sensitiveAuthAvailability = remember { sensitiveAuthAvailabilityProvider.availability() }

    val listState = rememberLazyListState()
    val contentTopPadding = rememberContentTopPadding()

    val title = stringResource(Res.string.settings_title)
    val cancelText = stringResource(Res.string.action_cancel)
    val themeDialogTitle = stringResource(Res.string.dialog_title_choose_theme)
    val biometricTitle = stringResource(Res.string.pref_biometric_title)
    val biometricReauthTitle = stringResource(Res.string.pref_biometric_reauth_title)
    val sensitiveAuthTitle = stringResource(Res.string.pref_sensitive_note_auth_frequency_title)

    val themeSystem = stringResource(Res.string.settings_theme_system)
    val themeLight = stringResource(Res.string.settings_theme_light)
    val themeDark = stringResource(Res.string.settings_theme_dark)
    val themeLabel: (AppTheme) -> String = { theme ->
        when (theme) {
            AppTheme.SYSTEM -> themeSystem
            AppTheme.LIGHT -> themeLight
            AppTheme.DARK -> themeDark
        }
    }
    val biometric30s = stringResource(Res.string.pref_biometric_reauth_30s)
    val biometric1m = stringResource(Res.string.pref_biometric_reauth_1m)
    val biometric5m = stringResource(Res.string.pref_biometric_reauth_5m)
    val biometric15m = stringResource(Res.string.pref_biometric_reauth_15m)
    val biometricIntervalLabel: (BiometricReAuthInterval) -> String = { interval ->
        when (interval) {
            BiometricReAuthInterval.THIRTY_SECONDS -> biometric30s
            BiometricReAuthInterval.ONE_MINUTE -> biometric1m
            BiometricReAuthInterval.FIVE_MINUTES -> biometric5m
            BiometricReAuthInterval.FIFTEEN_MINUTES -> biometric15m
        }
    }
    val sensitiveEveryTime = stringResource(Res.string.pref_sensitive_note_auth_frequency_every_time)
    val sensitiveOncePerStart =
        stringResource(Res.string.pref_sensitive_note_auth_frequency_once_per_app_start)
    val sensitiveNever = stringResource(Res.string.pref_sensitive_note_auth_frequency_never)
    val sensitiveFrequencyLabel: (SensitiveNoteAuthFrequency) -> String = { frequency ->
        when (frequency) {
            SensitiveNoteAuthFrequency.EVERY_TIME -> sensitiveEveryTime
            SensitiveNoteAuthFrequency.ONCE_PER_APP_START -> sensitiveOncePerStart
            SensitiveNoteAuthFrequency.NEVER -> sensitiveNever
        }
    }

    val clearDataAuthTitle = stringResource(Res.string.clear_data_auth_title)
    val clearDataAuthSubtitle = stringResource(Res.string.clear_data_auth_subtitle)
    val clearDataAuthFailed = stringResource(Res.string.msg_clear_data_auth_failed)
    val clearDataDialogTitle = stringResource(Res.string.clear_data_dialog_title)
    val clearDataDialogDesc = stringResource(Res.string.clear_data_dialog_desc)
    val clearDataDialogWarning = stringResource(Res.string.clear_data_dialog_warning)
    val clearDataDialogConfirm = stringResource(Res.string.action_clear_data_confirm)
    val clearDataSuccess = stringResource(Res.string.msg_clear_data_success)
    val unknownError = stringResource(Res.string.msg_unknown_error)

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsScreenContent(
            listState = listState,
            contentTopPadding = contentTopPadding,
            currentTheme = currentTheme,
            biometricReauthInterval = biometricReauthInterval,
            isBiometricDisabled = isBiometricDisabled,
            sensitiveNoteAuthFrequency = sensitiveNoteAuthFrequency,
            sensitiveAuthAvailability = sensitiveAuthAvailability,
            lastBackupFilename = lastBackupFilename,
            lastBackupTime = lastBackupTime,
            clearCacheEnabled = !isClearingData && !isClearDataAuthenticating,
            clearDataEnabled = !isClearingData && !isClearDataAuthenticating,
            onThemeClick = { showThemeDialog = true },
            onBiometricReauthClick = { showBiometricIntervalDialog = true },
            onSensitiveAuthClick = { showSensitiveNoteAuthDialog = true },
            onBackupSettingsClick = onBackupSettingsClick,
            onImportExportSettingsClick = onImportExportSettingsClick,
            onClearCacheClick = {
                if (isClearingData || isClearDataAuthenticating) return@SettingsScreenContent
                scope.launch {
                    val removedCount = clearMermaidSnapshotDiskCache(mermaidCacheIo)
                    userMessageSink.showShort(
                        getString(Res.string.msg_clear_preview_cache_success_fmt, removedCount)
                    )
                }
            },
            onClearDataClick = {
                if (isClearingData || isClearDataAuthenticating) return@SettingsScreenContent
                scope.launch {
                    isClearDataAuthenticating = true
                    try {
                        if (sensitiveAuthAvailabilityProvider.isAvailable()) {
                            val authenticated = sensitiveAuthPrompt.authenticate(
                                title = clearDataAuthTitle,
                                subtitle = clearDataAuthSubtitle
                            )
                            if (!authenticated) {
                                userMessageSink.showShort(clearDataAuthFailed)
                                return@launch
                            }
                        }
                        showClearDataDialog = true
                    } finally {
                        isClearDataAuthenticating = false
                    }
                }
            },
            onAboutClick = onAboutClick,
            biometricSwitch = {
                SettingsHapticSwitch(
                    checked = isBiometricEnabled,
                    onCheckedChange = { checked ->
                        scope.launch {
                            settingsRepository.setBiometricDisabled(!checked)
                        }
                    })
            },
            screenshotSwitch = {
                SettingsHapticSwitch(
                    checked = allowScreenshots,
                    onCheckedChange = { checked ->
                        scope.launch {
                            settingsRepository.setAllowScreenshots(checked)
                        }
                    }
                )
            },
            editorAutoSaveSwitch = {
                SettingsHapticSwitch(
                    checked = editorAutoSaveEnabled,
                    onCheckedChange = { checked ->
                        scope.launch {
                            settingsRepository.setEditorAutoSaveEnabled(checked)
                        }
                    })
            }
        )

        SettingsAppTopBar(
            title = title,
            onBack = onBack,
            listState = listState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentTheme = currentTheme,
                titleText = themeDialogTitle,
                cancelText = cancelText,
                optionText = themeLabel,
                onThemeSelected = { theme ->
                    scope.launch { settingsRepository.setAppTheme(theme) }
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
            )
        }

        if (showBiometricIntervalDialog) {
            BiometricIntervalSelectionDialog(
                currentInterval = biometricReauthInterval,
                titleText = biometricReauthTitle,
                cancelText = cancelText,
                optionText = biometricIntervalLabel,
                onSelected = { interval ->
                    scope.launch { settingsRepository.setBiometricReauthInterval(interval) }
                    showBiometricIntervalDialog = false
                },
                onDismiss = { showBiometricIntervalDialog = false }
            )
        }

        if (showSensitiveNoteAuthDialog) {
            SensitiveNoteAuthFrequencyDialog(
                current = sensitiveNoteAuthFrequency,
                titleText = sensitiveAuthTitle,
                cancelText = cancelText,
                optionText = sensitiveFrequencyLabel,
                onSelected = { frequency ->
                    scope.launch {
                        settingsRepository.setSensitiveNoteAuthFrequency(frequency)
                    }
                    showSensitiveNoteAuthDialog = false
                },
                onDismiss = { showSensitiveNoteAuthDialog = false }
            )
        }

        if (showClearDataDialog) {
            AppDialog(
                onDismissRequest = {
                    if (!isClearingData) showClearDataDialog = false
                },
                dismissOnBackPress = !isClearingData,
                dismissOnClickOutside = !isClearingData
            ) { triggerClose ->
                ClearDataConfirmDialog(
                    title = clearDataDialogTitle,
                    description = clearDataDialogDesc,
                    warning = clearDataDialogWarning,
                    confirmText = clearDataDialogConfirm,
                    cancelText = cancelText,
                    isBusy = isClearingData,
                    onCancel = {
                        if (!isClearingData) {
                            triggerClose { showClearDataDialog = false }
                        }
                    },
                    onConfirm = {
                        if (isClearingData) return@ClearDataConfirmDialog
                        isClearingData = true
                        scope.launch {
                            try {
                                noteRepository.clearAllNotes()
                                userMessageSink.showShort(clearDataSuccess)
                                triggerClose { showClearDataDialog = false }
                            } catch (e: Exception) {
                                val message = e.message ?: unknownError
                                userMessageSink.showShort(getString(Res.string.msg_clear_data_failed_fmt, message))
                            } finally {
                                isClearingData = false
                            }
                        }
                    }
                )
            }
        }
    }
}
