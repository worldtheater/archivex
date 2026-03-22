package com.worldtheater.archive.ui.widget

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.worldtheater.archive.ui.theme.WIDGET_CHIP_TEXT_DARK
import com.worldtheater.archive.ui.theme.WIDGET_CHIP_TEXT_LIGHT
import com.worldtheater.archive.ui.theme.isAppInDarkTheme


// Minimal UI Colors
val CHIP_TEXT_LIGHT = WIDGET_CHIP_TEXT_LIGHT
val CHIP_TEXT_DARK = WIDGET_CHIP_TEXT_DARK

private val MOVE_HINT_TEXT_DARK_A90 = Color(0xE6000000)
private val MOVE_HINT_TEXT_LIGHT_A90 = Color(0xE6FFFFFF)

private val SETTINGS_SECTION_CARD_LIGHT = Color(0x09101419)
private val SETTINGS_SECTION_CARD_DARK = Color(0x1AD9D9D9)
private val SETTINGS_ACTION_BUTTON_ENABLED_LIGHT = Color(0xFFE7E7E8)
private val SETTINGS_ACTION_BUTTON_ENABLED_DARK = Color(0xFF2F3033)
private val TOP_BAR_BG_LIGHT_A100 = Color(0xFFFFFCFA)
private val TOP_BAR_BG_LIGHT_A90 = Color(0xE6FFFCFA)
private val TOP_BAR_BG_LIGHT_A54 = Color(0x8AFFFCFA)
private val TOP_BAR_BG_DARK_A100 = Color(0xFF000000)
private val TOP_BAR_BG_DARK_A90 = Color(0xE6000000)
private val TOP_BAR_BG_DARK_A54 = Color(0x8A000000)

@Composable
fun dialogDimColor(): Color = if (isAppInDarkTheme()) Color(0xaa000000) else Color(0x77000000)

@Composable
fun capsuleBgColor(): Color = MaterialTheme.colorScheme.surface

@Composable
fun capsuleContentColor(): Color = MaterialTheme.colorScheme.onSurface

@Composable
fun moveHintTextA90(): Color =
    if (isAppInDarkTheme()) MOVE_HINT_TEXT_DARK_A90 else MOVE_HINT_TEXT_LIGHT_A90

@Composable
fun settingsSectionCardFixedColor(): Color =
    if (isAppInDarkTheme()) SETTINGS_SECTION_CARD_DARK else SETTINGS_SECTION_CARD_LIGHT

@Composable
fun settingsActionButtonFixedEnabledColor(): Color =
    if (isAppInDarkTheme()) SETTINGS_ACTION_BUTTON_ENABLED_DARK else SETTINGS_ACTION_BUTTON_ENABLED_LIGHT

@Composable
fun topBarBgA100(): Color = if (isAppInDarkTheme()) TOP_BAR_BG_DARK_A100 else TOP_BAR_BG_LIGHT_A100

@Composable
fun topBarBgA90(): Color = if (isAppInDarkTheme()) TOP_BAR_BG_DARK_A90 else TOP_BAR_BG_LIGHT_A90

@Composable
fun topBarBgA54(): Color = if (isAppInDarkTheme()) TOP_BAR_BG_DARK_A54 else TOP_BAR_BG_LIGHT_A54
