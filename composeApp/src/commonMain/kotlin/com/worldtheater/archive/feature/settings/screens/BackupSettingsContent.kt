package com.worldtheater.archive.feature.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import archivex.composeapp.generated.resources.*
import com.worldtheater.archive.feature.settings.components.SETTINGS_SECTION_CARD_RADIUS
import com.worldtheater.archive.feature.settings.components.SETTINGS_SECTION_HORIZONTAL_PADDING
import com.worldtheater.archive.feature.settings.components.SettingsItem
import com.worldtheater.archive.feature.settings.components.settingsSectionCardColor
import com.worldtheater.archive.ui.shape.SmoothRoundedCornerShape
import com.worldtheater.archive.ui.theme.appOnSurfaceA60
import org.jetbrains.compose.resources.stringResource

@Composable
fun BackupSettingsContent(
    listState: LazyListState,
    contentTopPadding: Dp,
    dirSubtitle: String,
    backupModeSubtitle: String,
    transferProgressText: String?,
    transferBusy: Boolean,
    canBackup: Boolean,
    lastBackupFilename: String?,
    onSelectBackupDir: () -> Unit,
    onSelectBackupMode: () -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    val backupDirTitle = stringResource(Res.string.pref_backup_dir_title)
    val backupModeTitle = stringResource(Res.string.pref_backup_mode_title)
    val backupButton = stringResource(Res.string.menu_backup)
    val restoreButton = stringResource(Res.string.menu_restore)

    Scaffold(
        content = { paddingValues ->
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = contentTopPadding,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp)
            ) {
                item {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = SETTINGS_SECTION_HORIZONTAL_PADDING)
                                .fillMaxWidth(),
                            shape = SmoothRoundedCornerShape(radius = SETTINGS_SECTION_CARD_RADIUS),
                            color = settingsSectionCardColor()
                        ) {
                            Column {
                                SettingsItem(
                                    title = backupDirTitle,
                                    subtitle = dirSubtitle,
                                    onClick = onSelectBackupDir
                                )
                                SettingsItem(
                                    title = backupModeTitle,
                                    subtitle = backupModeSubtitle,
                                    onClick = onSelectBackupMode
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = SETTINGS_SECTION_HORIZONTAL_PADDING)
                        ) {
                            BackupInfoSection(
                                lastBackupFilename = lastBackupFilename
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            if (transferProgressText != null) {
                                Text(
                                    text = transferProgressText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = appOnSurfaceA60(),
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            }

                            SettingsSecondaryActionButton(
                                text = backupButton,
                                enabled = canBackup && !transferBusy,
                                onClick = onBackupClick,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            SettingsSecondaryActionButton(
                                text = restoreButton,
                                enabled = !transferBusy,
                                onClick = onRestoreClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    )
}
