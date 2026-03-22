package com.worldtheater.archive.feature.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import archivex.composeapp.generated.resources.*
import com.worldtheater.archive.ui.theme.appErrorA70
import com.worldtheater.archive.ui.theme.appOnSurfaceA10
import com.worldtheater.archive.ui.theme.appOnSurfaceA60
import org.jetbrains.compose.resources.stringResource

@Composable
fun BackupInfoSection(
    lastBackupFilename: String?
) {
    val principles = stringResource(Res.string.backup_principles)
    val precautions = stringResource(Res.string.backup_precautions)
    val overwriteWarning = stringResource(Res.string.backup_restore_overwrite_warning)
    val statusLabel = stringResource(Res.string.backup_status_label)
    val statusNone = stringResource(Res.string.backup_status_none)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 6.dp)
    ) {
        Text(
            text = principles,
            style = MaterialTheme.typography.bodyMedium,
            color = appOnSurfaceA60(),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = precautions,
            style = MaterialTheme.typography.bodyMedium,
            color = appErrorA70(),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = overwriteWarning,
            style = MaterialTheme.typography.bodyMedium,
            color = appErrorA70(),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.material3.HorizontalDivider(color = appOnSurfaceA10())
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = statusLabel + (lastBackupFilename ?: statusNone),
            style = MaterialTheme.typography.bodySmall,
            color = appOnSurfaceA60()
        )
    }
}
