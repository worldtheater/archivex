package com.worldtheater.archive.feature.security

import androidx.compose.runtime.Composable
import com.worldtheater.archive.ui.widget.AppDialog
import com.worldtheater.archive.ui.widget.StackedIosAlert

@Composable
fun BiometricIssueDialog(
    title: String,
    description: String,
    goSettingsText: String,
    disableBiometricText: String,
    onGoSettings: () -> Unit,
    onDisableBiometric: () -> Unit
) {
    AppDialog(
        onDismissRequest = {},
        dismissOnBackPress = false,
        dismissOnClickOutside = false
    ) { triggerClose ->
        StackedIosAlert(
            title = title,
            description = description,
            onPrimaryClick = { triggerClose(onGoSettings) },
            primaryText = goSettingsText,
            onSecondaryClick = { triggerClose(onDisableBiometric) },
            secondaryText = disableBiometricText
        )
    }
}
