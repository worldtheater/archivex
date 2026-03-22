package com.worldtheater.archive.platform.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import archivex.composeapp.generated.resources.Res
import archivex.composeapp.generated.resources.ic_about_logo
import com.worldtheater.archive.feature.settings.screens.AboutScreenPlatformUi
import com.worldtheater.archive.feature.settings.screens.AboutScreenPlatformUiProvider
import com.worldtheater.archive.platform.system.isProdReleaseBuildByPlatform
import com.worldtheater.archive.ui.theme.isAppInDarkTheme
import org.jetbrains.compose.resources.painterResource

class DefaultAboutScreenPlatformUiProvider : AboutScreenPlatformUiProvider {
    @Composable
    override fun provide(
        onDebugToolsClick: (() -> Unit)?,
        appName: String
    ): AboutScreenPlatformUi {
        val isDarkTheme = isAppInDarkTheme()
        val logoBgColor = if (isDarkTheme) {
            MaterialTheme.colorScheme.inverseSurface
        } else {
            Color(0xFF000000)
        }
        val logoFgColor = if (isDarkTheme) {
            MaterialTheme.colorScheme.inverseOnSurface
        } else {
            Color.White
        }
        return AboutScreenPlatformUi(
            logoBgColor = logoBgColor,
            debugToolsEnabled = onDebugToolsClick != null && !isProdReleaseBuildByPlatform(),
            appLogo = { contentDescription ->
                Icon(
                    painter = painterResource(Res.drawable.ic_about_logo),
                    contentDescription = contentDescription,
                    tint = logoFgColor,
                    modifier = Modifier.size(68.dp)
                )
            }
        )
    }
}
