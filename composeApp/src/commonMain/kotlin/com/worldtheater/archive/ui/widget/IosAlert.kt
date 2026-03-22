package com.worldtheater.archive.ui.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldtheater.archive.ui.shape.SmoothRoundedCornerShape

@Composable
fun IosAlert(
    title: String,
    description: String? = null,
    titleOnlyBottomSpace: Dp = 24.dp,
    onPrimaryClick: () -> Unit,
    primaryText: String,
    primaryButtonEnabled: Boolean = true,
    onSecondaryClick: () -> Unit,
    secondaryText: String,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface, // Use theme surface color
        shape = SmoothRoundedCornerShape(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 18.dp)
        ) {
            AppDialogTitle(text = title)

            if (description != null) {
                Spacer(modifier = Modifier.height(6.dp))
                AppDialogDescription(text = description)
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(titleOnlyBottomSpace))
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (secondaryText.isNotEmpty()) 12.dp else 0.dp)
            ) {
                // Secondary Button (Cancel)
                if (secondaryText.isNotEmpty()) {
                    AppDialogSecondaryButton(
                        text = secondaryText,
                        onClick = onSecondaryClick,
                        modifier = Modifier
                            .weight(1f)
                    )
                }

                // Primary Button (Confirm)
                if (primaryText.isNotEmpty()) {
                    AppDialogPrimaryButton(
                        text = primaryText,
                        onClick = onPrimaryClick,
                        enabled = primaryButtonEnabled,
                        modifier = Modifier
                            .weight(1f),
                    )
                }
            }
        }
    }
}
