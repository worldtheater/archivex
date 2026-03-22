package com.worldtheater.archive.feature.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldtheater.archive.ui.shape.SmoothRoundedCornerShape
import com.worldtheater.archive.ui.theme.appErrorA70
import com.worldtheater.archive.ui.theme.appOnSurfaceA10
import com.worldtheater.archive.ui.theme.appOnSurfaceA60
import com.worldtheater.archive.ui.widget.AppDialogDescription
import com.worldtheater.archive.ui.widget.AppDialogPrimaryButton
import com.worldtheater.archive.ui.widget.AppDialogSecondaryButton
import com.worldtheater.archive.ui.widget.AppDialogTitle

@Composable
fun ClearDataConfirmDialog(
    title: String,
    description: String,
    warning: String,
    confirmText: String,
    cancelText: String,
    isBusy: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        shape = SmoothRoundedCornerShape(32.dp)
    ) {
        Column(
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppDialogTitle(text = title)
            AppDialogDescription(
                text = description,
                modifier = Modifier.padding(top = 8.dp),
                color = appOnSurfaceA60()
            )
            AppDialogDescription(
                text = warning,
                modifier = Modifier.padding(top = 8.dp),
                color = appErrorA70(),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))
            AppDialogPrimaryButton(
                text = confirmText,
                onClick = onConfirm,
                enabled = !isBusy,
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.error,
                disabledContainerColor = appOnSurfaceA10(),
                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onError,
                disabledContentColor = appOnSurfaceA60(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))
            AppDialogSecondaryButton(
                text = cancelText,
                onClick = onCancel,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SectionBlock(
    title: String,
    paddingTop: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = paddingTop, bottom = 8.dp),
        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = appOnSurfaceA60()
    )
    Surface(
        modifier = Modifier
            .padding(horizontal = SETTINGS_SECTION_HORIZONTAL_PADDING)
            .fillMaxWidth(),
        shape = SmoothRoundedCornerShape(radius = SETTINGS_SECTION_CARD_RADIUS),
        color = settingsSectionCardColor(),
        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    ) {
        Column(content = content)
    }
}
