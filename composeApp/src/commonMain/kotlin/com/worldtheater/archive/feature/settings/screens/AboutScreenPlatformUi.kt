package com.worldtheater.archive.feature.settings.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class AboutScreenPlatformUi(
    val logoBgColor: Color,
    val debugToolsEnabled: Boolean,
    val appLogo: @Composable (String) -> Unit
)

interface AboutScreenPlatformUiProvider {
    @Composable
    fun provide(
        onDebugToolsClick: (() -> Unit)?,
        appName: String
    ): AboutScreenPlatformUi
}
