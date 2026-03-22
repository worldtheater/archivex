package com.worldtheater.archive.feature.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import archivex.composeapp.generated.resources.*
import com.worldtheater.archive.domain.AppTheme
import com.worldtheater.archive.domain.BiometricReAuthInterval
import com.worldtheater.archive.domain.SensitiveNoteAuthFrequency
import com.worldtheater.archive.feature.settings.components.SETTINGS_SECTION_ITEM_SPACING
import com.worldtheater.archive.feature.settings.components.SectionBlock
import com.worldtheater.archive.feature.settings.components.SettingsItem
import com.worldtheater.archive.platform.auth.SensitiveAuthAvailability
import com.worldtheater.archive.platform.system.defaultRelativeTimeFormatter
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreenContent(
    listState: LazyListState,
    contentTopPadding: Dp,
    currentTheme: AppTheme,
    biometricReauthInterval: BiometricReAuthInterval,
    isBiometricDisabled: Boolean,
    sensitiveNoteAuthFrequency: SensitiveNoteAuthFrequency,
    sensitiveAuthAvailability: SensitiveAuthAvailability,
    lastBackupFilename: String?,
    lastBackupTime: Long,
    clearCacheEnabled: Boolean,
    clearDataEnabled: Boolean,
    onThemeClick: () -> Unit,
    onBiometricReauthClick: () -> Unit,
    onSensitiveAuthClick: () -> Unit,
    onBackupSettingsClick: () -> Unit,
    onImportExportSettingsClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    onClearDataClick: () -> Unit,
    onAboutClick: () -> Unit,
    biometricSwitch: @Composable () -> Unit,
    screenshotSwitch: @Composable () -> Unit,
    editorAutoSaveSwitch: @Composable () -> Unit
) {
    val isBiometricEnabled = !isBiometricDisabled
    val appearanceSection = stringResource(Res.string.settings_section_appearance)
    val themeTitle = stringResource(Res.string.settings_theme_title)
    val securitySection = stringResource(Res.string.settings_section_security)
    val biometricTitle = stringResource(Res.string.pref_biometric_title)
    val biometricSubtitle = stringResource(
        if (isBiometricEnabled) {
            Res.string.pref_biometric_summary_enabled
        } else {
            Res.string.pref_biometric_summary_disabled
        }
    )
    val biometricReauthTitle = stringResource(Res.string.pref_biometric_reauth_title)
    val sensitiveAuthTitle = stringResource(Res.string.pref_sensitive_note_auth_frequency_title)
    val allowScreenshotsTitle = stringResource(Res.string.pref_allow_screenshots_title)
    val allowScreenshotsSummary = stringResource(Res.string.pref_allow_screenshots_summary)
    val editorSection = stringResource(Res.string.settings_section_editor)
    val editorAutoSaveTitle = stringResource(Res.string.pref_editor_auto_save_title)
    val editorAutoSaveSubtitle = stringResource(Res.string.pref_editor_auto_save_summary)
    val dataSection = stringResource(Res.string.settings_section_data)
    val backupRestoreTitle = stringResource(Res.string.pref_backup_restore_title)
    val importExportTitle = stringResource(Res.string.pref_import_export_title)
    val importExportSubtitle = stringResource(Res.string.pref_import_export_summary)
    val clearCacheTitle = stringResource(Res.string.pref_clear_preview_cache_title)
    val clearCacheSubtitle = stringResource(Res.string.pref_clear_preview_cache_summary)
    val clearDataTitle = stringResource(Res.string.pref_clear_all_data_title)
    val clearDataSubtitle = stringResource(Res.string.pref_clear_all_data_summary)
    val aboutSection = stringResource(Res.string.settings_section_about)
    val aboutTitle = stringResource(Res.string.pref_about_title)
    val aboutSubtitle = stringResource(Res.string.pref_about_summary)
    val themeSummary = when (currentTheme) {
        AppTheme.SYSTEM -> stringResource(Res.string.settings_theme_system)
        AppTheme.LIGHT -> stringResource(Res.string.settings_theme_light)
        AppTheme.DARK -> stringResource(Res.string.settings_theme_dark)
    }
    val biometricIntervalText = when (biometricReauthInterval) {
        BiometricReAuthInterval.THIRTY_SECONDS -> stringResource(Res.string.pref_biometric_reauth_30s)
        BiometricReAuthInterval.ONE_MINUTE -> stringResource(Res.string.pref_biometric_reauth_1m)
        BiometricReAuthInterval.FIVE_MINUTES -> stringResource(Res.string.pref_biometric_reauth_5m)
        BiometricReAuthInterval.FIFTEEN_MINUTES -> stringResource(Res.string.pref_biometric_reauth_15m)
    }
    val biometricReauthSummary = stringResource(
        Res.string.pref_biometric_reauth_summary_fmt,
        biometricIntervalText
    )
    val sensitiveFrequencyText = when (sensitiveNoteAuthFrequency) {
        SensitiveNoteAuthFrequency.EVERY_TIME ->
            stringResource(Res.string.pref_sensitive_note_auth_frequency_every_time)

        SensitiveNoteAuthFrequency.ONCE_PER_APP_START ->
            stringResource(Res.string.pref_sensitive_note_auth_frequency_once_per_app_start)

        SensitiveNoteAuthFrequency.NEVER ->
            stringResource(Res.string.pref_sensitive_note_auth_frequency_never)
    }
    val sensitiveAuthSummary = when (sensitiveAuthAvailability) {
        SensitiveAuthAvailability.AVAILABLE -> stringResource(
            Res.string.pref_sensitive_note_auth_frequency_summary_fmt,
            sensitiveFrequencyText
        )

        SensitiveAuthAvailability.NO_HARDWARE ->
            stringResource(Res.string.pref_sensitive_note_auth_unavailable_no_hardware)

        SensitiveAuthAvailability.HARDWARE_UNAVAILABLE ->
            stringResource(Res.string.pref_sensitive_note_auth_unavailable_hw_unavailable)

        SensitiveAuthAvailability.NO_CREDENTIAL ->
            stringResource(Res.string.pref_sensitive_note_auth_unavailable_no_credential)

        SensitiveAuthAvailability.UNAVAILABLE ->
            stringResource(Res.string.pref_sensitive_note_auth_unavailable_generic)
    }
    val relativeTime = defaultRelativeTimeFormatter().format(lastBackupTime)
    val backupSubtitle = if (relativeTime != null) {
        stringResource(Res.string.pref_last_backup_fmt, relativeTime)
    } else if (!lastBackupFilename.isNullOrBlank()) {
        stringResource(Res.string.pref_last_backup_fmt, lastBackupFilename)
    } else {
        stringResource(Res.string.pref_backup_summary)
    }
    val isSensitiveAuthAvailable = sensitiveAuthAvailability == SensitiveAuthAvailability.AVAILABLE

    Scaffold(
        content = { paddingValues ->
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = contentTopPadding,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                ),
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            ) {
                item {
                    Column {
                        SectionBlock(
                            title = appearanceSection,
                            paddingTop = 8.dp
                        ) {
                            SettingsItem(
                                title = themeTitle,
                                subtitle = themeSummary,
                                onClick = onThemeClick
                            )
                        }

                        SectionBlock(title = securitySection) {
                            SettingsItem(
                                title = biometricTitle,
                                subtitle = biometricSubtitle,
                                action = {
                                    Spacer(modifier = androidx.compose.ui.Modifier.width(16.dp))
                                    biometricSwitch()
                                }
                            )
                            Spacer(modifier = androidx.compose.ui.Modifier.height(SETTINGS_SECTION_ITEM_SPACING))
                            SettingsItem(
                                title = biometricReauthTitle,
                                subtitle = biometricReauthSummary,
                                enabled = isBiometricEnabled,
                                onClick = onBiometricReauthClick
                            )
                            Spacer(modifier = androidx.compose.ui.Modifier.height(SETTINGS_SECTION_ITEM_SPACING))
                            SettingsItem(
                                title = sensitiveAuthTitle,
                                subtitle = sensitiveAuthSummary,
                                enabled = isSensitiveAuthAvailable,
                                onClick = onSensitiveAuthClick
                            )
                            Spacer(modifier = androidx.compose.ui.Modifier.height(SETTINGS_SECTION_ITEM_SPACING))
                            SettingsItem(
                                title = allowScreenshotsTitle,
                                subtitle = allowScreenshotsSummary,
                                action = {
                                    Spacer(modifier = androidx.compose.ui.Modifier.width(16.dp))
                                    screenshotSwitch()
                                }
                            )
                        }

                        SectionBlock(title = editorSection) {
                            SettingsItem(
                                title = editorAutoSaveTitle,
                                subtitle = editorAutoSaveSubtitle,
                                action = {
                                    Spacer(modifier = androidx.compose.ui.Modifier.width(16.dp))
                                    editorAutoSaveSwitch()
                                }
                            )
                        }

                        SectionBlock(title = dataSection) {
                            SettingsItem(
                                title = backupRestoreTitle,
                                subtitle = backupSubtitle,
                                onClick = onBackupSettingsClick
                            )
                            Spacer(modifier = androidx.compose.ui.Modifier.height(SETTINGS_SECTION_ITEM_SPACING))
                            SettingsItem(
                                title = importExportTitle,
                                subtitle = importExportSubtitle,
                                onClick = onImportExportSettingsClick
                            )
                            Spacer(modifier = androidx.compose.ui.Modifier.height(SETTINGS_SECTION_ITEM_SPACING))
                            SettingsItem(
                                title = clearCacheTitle,
                                subtitle = clearCacheSubtitle,
                                enabled = clearCacheEnabled,
                                onClick = onClearCacheClick
                            )
                            Spacer(modifier = androidx.compose.ui.Modifier.height(SETTINGS_SECTION_ITEM_SPACING))
                            SettingsItem(
                                title = clearDataTitle,
                                subtitle = clearDataSubtitle,
                                enabled = clearDataEnabled,
                                onClick = onClearDataClick
                            )
                        }

                        SectionBlock(title = aboutSection) {
                            SettingsItem(
                                title = aboutTitle,
                                subtitle = aboutSubtitle,
                                onClick = onAboutClick
                            )
                        }
                    }
                }
            }
        }
    )
}
