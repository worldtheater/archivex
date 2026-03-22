package com.worldtheater.archive.feature.settings.components

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.worldtheater.archive.platform.gateway.HapticFeedbackGateway
import org.koin.compose.koinInject

@Composable
fun SettingsHapticSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val performHaptic = rememberSettingsSwitchHapticPerformer()
    Switch(
        checked = checked,
        onCheckedChange = { newChecked ->
            performHaptic(newChecked)
            onCheckedChange(newChecked)
        },
        modifier = modifier,
        enabled = enabled
    )
}

@Composable
fun rememberSettingsSwitchHapticPerformer(): (Boolean) -> Unit {
    val hapticGateway: HapticFeedbackGateway = koinInject()
    return remember(hapticGateway) {
        { checked ->
            hapticGateway.vibrate(
                milliseconds = if (checked) 24L else 12L,
                effect = if (checked) 180 else 40
            )
        }
    }
}
