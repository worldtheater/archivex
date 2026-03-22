package com.worldtheater.archive.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.worldtheater.archive.ui.shape.Shapes

private val DarkColorScheme = darkColorScheme(
    primary = LIGHT_SNOW,
    secondary = SIZZLING_RED,
    error = THEME_DARK_ERROR,
    onError = Color.White,
    errorContainer = THEME_DARK_ERROR_CONTAINER,
    onErrorContainer = THEME_DARK_ON_ERROR_CONTAINER,
    background = Color.Black,
    surface = THEME_DARK_SURFACE,
    surfaceVariant = THEME_DARK_SURFACE_VARIANT,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = ASH,
    onSurface = ASH,
    onSurfaceVariant = THEME_DARK_ON_SURFACE_VARIANT
)

private val LightColorScheme = lightColorScheme(
    primary = DARK_GREY_2,
    secondary = LIGHT_TEAL,
    error = THEME_LIGHT_ERROR,
    onError = Color.White,
    errorContainer = THEME_LIGHT_ERROR_CONTAINER,
    onErrorContainer = THEME_LIGHT_ON_ERROR_CONTAINER,
    background = SNOW,
    surface = Color.White,
    surfaceVariant = THEME_LIGHT_SURFACE_VARIANT,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = DARKSOUL,
    onSurface = DARKSOUL,
    onSurfaceVariant = THEME_LIGHT_ON_SURFACE_VARIANT
)

@Composable
fun ArchiveThemeCore(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
    ) {
        CompositionLocalProvider(LocalAppDarkTheme provides darkTheme) {
            content()
        }
    }
}
