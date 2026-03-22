package com.worldtheater.archive.ui.widget

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldtheater.archive.ui.theme.appOnSurfaceA20
import com.worldtheater.archive.ui.theme.appOnSurfaceA30

@Composable
fun MarkdownUndoRedoLeadingActions(
    canUndo: Boolean,
    canRedo: Boolean,
    undoDescription: String,
    redoDescription: String,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    IconButton(onClick = onUndo, enabled = canUndo) {
        Icon(
            Icons.AutoMirrored.Filled.Undo,
            contentDescription = undoDescription,
            tint = if (canUndo) {
                MaterialTheme.colorScheme.onSurface
            } else {
                appOnSurfaceA30()
            }
        )
    }
    IconButton(onClick = onRedo, enabled = canRedo) {
        Icon(
            Icons.AutoMirrored.Filled.Redo,
            contentDescription = redoDescription,
            tint = if (canRedo) {
                MaterialTheme.colorScheme.onSurface
            } else {
                appOnSurfaceA30()
            }
        )
    }
    VerticalDivider(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
        color = appOnSurfaceA20(),
        thickness = 1.dp
    )
}

