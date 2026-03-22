package com.worldtheater.archive.feature.settings.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import archivex.composeapp.generated.resources.*
import com.worldtheater.archive.domain.BackupSecurityMode
import com.worldtheater.archive.domain.repository.SettingsRepository
import com.worldtheater.archive.feature.note_list.components.dialogs.PasswordDialog
import com.worldtheater.archive.feature.note_list.components.dialogs.RestorePasswordDialog
import com.worldtheater.archive.feature.note_list.screens.NoteListViewModel
import com.worldtheater.archive.platform.auth.SensitiveAuthAvailabilityProvider
import com.worldtheater.archive.platform.auth.SensitiveAuthPrompt
import com.worldtheater.archive.platform.gateway.BackupDirectoryGateway
import com.worldtheater.archive.platform.gateway.BackupDocumentPickerGateway
import com.worldtheater.archive.platform.system.UserMessageSink
import com.worldtheater.archive.security.PasswordStrengthLevel
import com.worldtheater.archive.ui.theme.rememberContentTopPadding
import com.worldtheater.archive.ui.widget.SettingsAppTopBar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private sealed interface BackupDialogState {
    data object None : BackupDialogState
    data object SelectBackupMode : BackupDialogState
    data class BackupPassword(val mode: BackupSecurityMode) : BackupDialogState
    data class RestorePassword(val uri: String) : BackupDialogState
}

@Composable
fun BackupSettingsScreen(
    viewModel: NoteListViewModel,
    onBack: () -> Unit
) {
    val settingsRepository: SettingsRepository = koinInject()
    val sensitiveAuthAvailabilityProvider: SensitiveAuthAvailabilityProvider = koinInject()
    val sensitiveAuthPrompt: SensitiveAuthPrompt = koinInject()
    val userMessageSink: UserMessageSink = koinInject()
    val unknownErrorText = stringResource(Res.string.msg_unknown_error)
    val savePermissionFailedPrefix = stringResource(Res.string.msg_save_permission_failed_prefix)
    val backupDirNotSet = stringResource(Res.string.pref_backup_dir_not_set)
    val topBarTitle = stringResource(Res.string.pref_backup_restore_title)
    val authTitle = stringResource(Res.string.backup_restore_auth_title)
    val authSubtitle = stringResource(Res.string.backup_restore_auth_subtitle)
    val authFailedMessage = stringResource(Res.string.msg_backup_restore_auth_failed)
    val backupTitle = stringResource(Res.string.menu_backup)
    val backupDescription = stringResource(Res.string.msg_backup_password_prompt)
    val restoreTitle = stringResource(Res.string.title_decrypt_restore)
    val restoreDescription = stringResource(Res.string.msg_restore_password_prompt)
    val confirmText = stringResource(Res.string.action_confirm)
    val cancelText = stringResource(Res.string.action_cancel)
    val passwordLabel = stringResource(Res.string.password_label)
    val confirmPasswordLabel = stringResource(Res.string.confirm_password_label)
    val passwordMismatchText = stringResource(Res.string.msg_password_mismatch)
    val passwordPolicyNotMetText = stringResource(Res.string.msg_password_policy_not_met)
    val platformActions = rememberBackupPlatformActions(
        settingsRepository = settingsRepository,
        userMessageSink = userMessageSink,
        unknownErrorText = unknownErrorText,
        savePermissionFailedPrefix = savePermissionFailedPrefix,
        backupDirectoryGateway = koinInject(),
        backupDocumentPickerGateway = koinInject()
    )
    val scope = rememberCoroutineScope()

    val backupDir by settingsRepository.backupDirFlow.collectAsState()
    val backupSecurityMode by settingsRepository.backupSecurityModeFlow.collectAsState()
    val lastBackupFilename by settingsRepository.lastBackupFilenameFlow.collectAsState()
    val transferProgress by viewModel.transferProgress.collectAsState()
    val listState = rememberLazyListState()
    val contentTopPadding = rememberContentTopPadding()
    var dialogState by remember { mutableStateOf<BackupDialogState>(BackupDialogState.None) }

    val transferBusy = transferProgress != null
    val transferProgressText = transferProgress?.let { progress ->
        val title = when (progress.type) {
            NoteListViewModel.TransferType.BACKUP -> stringResource(Res.string.transfer_backup_running)
            NoteListViewModel.TransferType.RESTORE -> stringResource(Res.string.transfer_restore_running)
            NoteListViewModel.TransferType.EXPORT_PLAIN -> stringResource(Res.string.transfer_export_running)
            NoteListViewModel.TransferType.IMPORT_PLAIN -> stringResource(Res.string.transfer_import_running)
        }
        if (progress.totalSteps > 0) {
            stringResource(Res.string.msg_transfer_progress_fmt, title, progress.completedSteps, progress.totalSteps)
        } else {
            title
        }
    }

    val dirSubtitle = platformActions.dirSubtitle(backupDir, backupDirNotSet)
    val backupModeSubtitle = when (backupSecurityMode) {
        BackupSecurityMode.DEVICE_BOUND -> stringResource(Res.string.backup_mode_device_bound_title)
        BackupSecurityMode.CROSS_DEVICE -> stringResource(Res.string.backup_mode_cross_device_title)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BackupSettingsContent(
            listState = listState,
            contentTopPadding = contentTopPadding,
            dirSubtitle = dirSubtitle,
            backupModeSubtitle = backupModeSubtitle,
            transferProgressText = transferProgressText,
            transferBusy = transferBusy,
            canBackup = backupDir != null,
            lastBackupFilename = lastBackupFilename,
            onSelectBackupDir = platformActions.onSelectBackupDir,
            onSelectBackupMode = { dialogState = BackupDialogState.SelectBackupMode },
            onBackupClick = {
                scope.launch {
                    if (sensitiveAuthAvailabilityProvider.isAvailable()) {
                        val authenticated = sensitiveAuthPrompt.authenticate(
                            title = authTitle,
                            subtitle = authSubtitle
                        )
                        if (!authenticated) {
                            userMessageSink.showShort(authFailedMessage)
                            return@launch
                        }
                    }
                    dialogState = BackupDialogState.BackupPassword(backupSecurityMode)
                }
            },
            onRestoreClick = {
                scope.launch {
                    if (sensitiveAuthAvailabilityProvider.isAvailable()) {
                        val authenticated = sensitiveAuthPrompt.authenticate(
                            title = authTitle,
                            subtitle = authSubtitle
                        )
                        if (!authenticated) {
                            userMessageSink.showShort(authFailedMessage)
                            return@launch
                        }
                    }
                    val uri = platformActions.requestRestoreDocument() ?: return@launch
                    dialogState = BackupDialogState.RestorePassword(uri)
                }
            }
        )

        SettingsAppTopBar(
            title = topBarTitle,
            onBack = onBack,
            listState = listState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        when (val state = dialogState) {
            BackupDialogState.None -> Unit
            BackupDialogState.SelectBackupMode -> {
                BackupModeDialogContent(
                    selectedMode = backupSecurityMode,
                    onDismiss = { dialogState = BackupDialogState.None },
                    onConfirm = { mode ->
                        scope.launch { settingsRepository.setBackupSecurityMode(mode) }
                        dialogState = BackupDialogState.None
                    }
                )
            }

            is BackupDialogState.BackupPassword -> {
                PasswordDialog(
                    title = backupTitle,
                    description = backupDescription,
                    confirmText = confirmText,
                    cancelText = cancelText,
                    passwordLabel = passwordLabel,
                    confirmPasswordLabel = confirmPasswordLabel,
                    passwordMismatchText = passwordMismatchText,
                    passwordPolicyNotMetText = passwordPolicyNotMetText,
                    passwordIllegalCharsFormatter = { unsupported ->
                        stringResource(Res.string.msg_password_illegal_chars_fmt, unsupported)
                    },
                    passwordStrengthFormatter = { level ->
                        val levelTextRes = when (level) {
                            PasswordStrengthLevel.VERY_WEAK -> Res.string.password_strength_very_weak
                            PasswordStrengthLevel.WEAK -> Res.string.password_strength_weak
                            PasswordStrengthLevel.FAIR -> Res.string.password_strength_fair
                            PasswordStrengthLevel.GOOD -> Res.string.password_strength_good
                            PasswordStrengthLevel.STRONG -> Res.string.password_strength_strong
                        }
                        stringResource(Res.string.password_strength_fmt, stringResource(levelTextRes))
                    },
                    onDismiss = { dialogState = BackupDialogState.None },
                    onConfirm = { pwd ->
                        if (pwd.isNotBlank()) {
                            viewModel.backupToDirectory(pwd = pwd, mode = state.mode)
                            dialogState = BackupDialogState.None
                        }
                    }
                )
            }

            is BackupDialogState.RestorePassword -> {
                RestorePasswordDialog(
                    title = restoreTitle,
                    description = restoreDescription,
                    confirmText = confirmText,
                    cancelText = cancelText,
                    passwordLabel = passwordLabel,
                    onDismiss = { dialogState = BackupDialogState.None },
                    onConfirm = { pwd ->
                        if (pwd.isNotBlank()) {
                            viewModel.restore(state.uri, pwd)
                            dialogState = BackupDialogState.None
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun rememberBackupPlatformActions(
    settingsRepository: SettingsRepository,
    userMessageSink: UserMessageSink,
    unknownErrorText: String,
    savePermissionFailedPrefix: String,
    backupDirectoryGateway: BackupDirectoryGateway,
    backupDocumentPickerGateway: BackupDocumentPickerGateway
): BackupPlatformActions {
    val scope = rememberCoroutineScope()
    val pickerActions = backupDocumentPickerGateway.rememberActions(
        onBackupDirPicked = { uriString ->
            if (uriString == null) return@rememberActions
            scope.launch {
                try {
                    backupDirectoryGateway.persistBackupDirectoryPermission(uriString)
                    settingsRepository.setBackupDir(uriString)
                } catch (e: Exception) {
                    val errorMsg = e.message ?: unknownErrorText
                    userMessageSink.showShort("$savePermissionFailedPrefix: $errorMsg")
                }
            }
        }
    )
    return BackupPlatformActions(
        dirSubtitle = { backupDir, emptyText ->
            if (backupDir != null) {
                backupDirectoryGateway.readableBackupDirectoryPath(backupDir)
            } else {
                emptyText
            }
        },
        onSelectBackupDir = pickerActions.launchBackupDirPicker,
        requestRestoreDocument = pickerActions.requestRestoreDocument
    )
}
