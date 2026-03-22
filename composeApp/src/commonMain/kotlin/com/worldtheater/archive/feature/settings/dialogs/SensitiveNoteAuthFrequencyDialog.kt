package com.worldtheater.archive.feature.settings.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.worldtheater.archive.domain.SensitiveNoteAuthFrequency
import com.worldtheater.archive.ui.shape.SmoothRoundedCornerShape
import com.worldtheater.archive.ui.theme.appOnSurfaceA50
import com.worldtheater.archive.ui.widget.AppDialog
import com.worldtheater.archive.ui.widget.IosAlert

@Composable
fun SensitiveNoteAuthFrequencyDialog(
    current: SensitiveNoteAuthFrequency,
    titleText: String,
    cancelText: String,
    optionText: (SensitiveNoteAuthFrequency) -> String,
    onSelected: (SensitiveNoteAuthFrequency) -> Unit,
    onDismiss: () -> Unit
) {
    AppDialog(onDismissRequest = onDismiss) { triggerClose ->
        IosAlert(
            title = titleText,
            titleOnlyBottomSpace = 12.dp,
            primaryText = "",
            onPrimaryClick = {},
            secondaryText = cancelText,
            onSecondaryClick = { triggerClose { onDismiss() } }
        ) {
            Column(Modifier.selectableGroup()) {
                SensitiveNoteAuthFrequency.entries.forEach { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .height(50.dp)
                            .clip(SmoothRoundedCornerShape(14.dp))
                            .selectable(
                                selected = item == current,
                                onClick = { triggerClose { onSelected(item) } },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = item == current,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = appOnSurfaceA50()
                            )
                        )
                        Text(
                            text = optionText(item),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
