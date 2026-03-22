package com.worldtheater.archive.feature.settings.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import archivex.composeapp.generated.resources.*
import com.worldtheater.archive.domain.BackupSecurityMode
import com.worldtheater.archive.ui.shape.SmoothRoundedCornerShape
import com.worldtheater.archive.ui.theme.appOnSurfaceA05
import com.worldtheater.archive.ui.theme.appOnSurfaceA60
import com.worldtheater.archive.ui.widget.AppDialog
import com.worldtheater.archive.ui.widget.IosAlert
import org.jetbrains.compose.resources.stringResource

@Composable
fun BackupModeDialogContent(
    selectedMode: BackupSecurityMode,
    onDismiss: () -> Unit,
    onConfirm: (BackupSecurityMode) -> Unit
) {
    val title = stringResource(Res.string.backup_mode_dialog_title)
    val description = stringResource(Res.string.backup_mode_dialog_desc)
    val confirm = stringResource(Res.string.action_confirm)
    val cancel = stringResource(Res.string.action_cancel)
    val deviceBoundTitle = stringResource(Res.string.backup_mode_device_bound_title)
    val deviceBoundDescription = stringResource(Res.string.backup_mode_device_bound_desc)
    val crossDeviceTitle = stringResource(Res.string.backup_mode_cross_device_title)
    val crossDeviceDescription = stringResource(Res.string.backup_mode_cross_device_desc)

    AppDialog(onDismissRequest = onDismiss, enableSlideAnimation = true) { triggerClose ->
        var pendingSelection by remember(selectedMode) { mutableStateOf(selectedMode) }
        IosAlert(
            title = title,
            description = description,
            primaryText = confirm,
            onPrimaryClick = {
                triggerClose { onConfirm(pendingSelection) }
            },
            secondaryText = cancel,
            onSecondaryClick = { triggerClose { onDismiss() } }
        ) {
            BackupModeOption(
                title = deviceBoundTitle,
                summary = deviceBoundDescription,
                selected = pendingSelection == BackupSecurityMode.DEVICE_BOUND,
                onClick = { pendingSelection = BackupSecurityMode.DEVICE_BOUND }
            )
            Spacer(modifier = Modifier.height(8.dp))
            BackupModeOption(
                title = crossDeviceTitle,
                summary = crossDeviceDescription,
                selected = pendingSelection == BackupSecurityMode.CROSS_DEVICE,
                onClick = { pendingSelection = BackupSecurityMode.CROSS_DEVICE }
            )
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun BackupModeOption(
    title: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SmoothRoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = SmoothRoundedCornerShape(16.dp),
        color = if (selected) appOnSurfaceA05() else Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                RadioButton(selected = selected, onClick = onClick)
            }
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = appOnSurfaceA60()
            )
        }
    }
}
