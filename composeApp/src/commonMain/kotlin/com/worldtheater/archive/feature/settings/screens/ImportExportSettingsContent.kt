package com.worldtheater.archive.feature.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import archivex.composeapp.generated.resources.*
import com.worldtheater.archive.feature.settings.components.SETTINGS_SECTION_HORIZONTAL_PADDING
import com.worldtheater.archive.feature.settings.components.settingsActionButtonColor
import com.worldtheater.archive.ui.shape.SmoothCapsuleShape
import com.worldtheater.archive.ui.theme.appErrorA70
import com.worldtheater.archive.ui.theme.appOnSurfaceA10
import com.worldtheater.archive.ui.theme.appOnSurfaceA30
import com.worldtheater.archive.ui.theme.appOnSurfaceA60
import org.jetbrains.compose.resources.stringResource

@Composable
fun ImportExportSettingsContent(
    listState: LazyListState,
    contentTopPadding: Dp,
    transferProgressText: String?,
    transferBusy: Boolean,
    onImportPlainClick: () -> Unit,
    onExportPlainClick: () -> Unit
) {
    val importPlain = stringResource(Res.string.menu_import_plain)
    val exportPlain = stringResource(Res.string.menu_export_plain)

    Scaffold(
        content = { paddingValues ->
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = contentTopPadding,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SETTINGS_SECTION_HORIZONTAL_PADDING)
                    ) {
                        ImportExportInfoBlock()

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
                            text = importPlain,
                            enabled = !transferBusy,
                            onClick = onImportPlainClick,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        SettingsSecondaryActionButton(
                            text = exportPlain,
                            enabled = !transferBusy,
                            onClick = onExportPlainClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun ImportExportInfoBlock() {
    val importNotice1 = stringResource(Res.string.import_export_notice_1)
    val importNotice2 = stringResource(Res.string.import_export_notice_2)
    val importNotice3 = stringResource(Res.string.import_export_notice_3)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 6.dp)
    ) {
        Text(
            text = importNotice1,
            style = MaterialTheme.typography.bodyMedium,
            color = appOnSurfaceA60(),
            fontSize = 14.sp
        )
        Text(
            text = importNotice2,
            style = MaterialTheme.typography.bodyMedium,
            color = appOnSurfaceA60(),
            fontSize = 14.sp
        )
        Text(
            text = importNotice3,
            style = MaterialTheme.typography.bodyMedium,
            color = appErrorA70(),
            fontSize = 14.sp
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = appOnSurfaceA10()
        )
    }
}

@Composable
fun SettingsSecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(SmoothCapsuleShape())
            .background(settingsActionButtonColor(enabled))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else appOnSurfaceA30(),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
