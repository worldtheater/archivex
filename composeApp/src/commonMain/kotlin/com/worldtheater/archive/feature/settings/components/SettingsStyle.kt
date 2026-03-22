package com.worldtheater.archive.feature.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.worldtheater.archive.ui.widget.settingsActionButtonFixedEnabledColor
import com.worldtheater.archive.ui.widget.settingsSectionCardFixedColor

val SETTINGS_SECTION_CARD_RADIUS = 20.dp
val SETTINGS_SECTION_HORIZONTAL_PADDING = 12.dp
val SETTINGS_SECTION_ITEM_SPACING = 4.dp

@Composable
fun settingsSectionCardColor(): Color = settingsSectionCardFixedColor()

@Composable
fun settingsActionButtonColor(enabled: Boolean): Color =
    settingsActionButtonFixedEnabledColor().copy(alpha = if (enabled) 1f else 0.55f)
