package com.worldtheater.archive.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldtheater.archive.ui.shape.SmoothRoundedCornerShape

@Composable
fun StackedIosAlert(
    title: String,
    description: String,
    onPrimaryClick: () -> Unit,
    primaryText: String,
    onSecondaryClick: () -> Unit,
    secondaryText: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = SmoothRoundedCornerShape(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 18.dp)
        ) {
            AppDialogTitle(text = title)

            AppDialogDescription(
                text = description,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppDialogPrimaryButton(
                    text = primaryText,
                    onClick = onPrimaryClick,
                    modifier = Modifier.fillMaxWidth()
                )

                AppDialogSecondaryButton(
                    text = secondaryText,
                    onClick = onSecondaryClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
