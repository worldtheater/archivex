package com.worldtheater.archive.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import java.awt.Color
import java.awt.Window
import java.lang.ref.WeakReference
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JRootPane
import javax.swing.SwingUtilities

@Composable
actual fun ApplyPlatformThemeSideEffect(darkTheme: Boolean) {
    SideEffect {
        DesktopWindowThemeController.applyTheme(darkTheme)
    }
}

object DesktopWindowThemeController {
    private var currentWindowRef: WeakReference<Window>? = null

    fun attach(window: Window) {
        currentWindowRef = WeakReference(window)
    }

    fun detach(window: Window) {
        if (currentWindowRef?.get() === window) {
            currentWindowRef = null
        }
    }

    fun applyTheme(darkTheme: Boolean) {
        val window = currentWindowRef?.get() ?: return
        if (SwingUtilities.isEventDispatchThread()) {
            applyThemeOnEdt(window, darkTheme)
        } else {
            SwingUtilities.invokeLater {
                applyThemeOnEdt(window, darkTheme)
            }
        }
    }

    private fun applyThemeOnEdt(window: Window, darkTheme: Boolean) {
        val background = if (darkTheme) Color(0x12, 0x12, 0x12) else Color(0xF7, 0xF7, 0xF7)
        val foreground = if (darkTheme) Color(0xF2, 0xF2, 0xF2) else Color(0x16, 0x16, 0x16)

        window.background = background
        window.foreground = foreground

        val rootPane = when (window) {
            is JFrame -> window.rootPane
            is JDialog -> window.rootPane
            else -> null
        }

        rootPane?.let {
            applyRootPaneTheme(it, darkTheme, background, foreground)
        }

        window.repaint()
    }

    private fun applyRootPaneTheme(
        rootPane: JRootPane,
        darkTheme: Boolean,
        background: Color,
        foreground: Color
    ) {
        rootPane.putClientProperty(
            "apple.awt.windowAppearance",
            if (darkTheme) "NSAppearanceNameDarkAqua" else "NSAppearanceNameAqua"
        )
        rootPane.putClientProperty("jetbrains.awt.windowDarkAppearance", darkTheme)
        rootPane.putClientProperty("JRootPane.titleBarBackground", background)
        rootPane.putClientProperty("JRootPane.titleBarForeground", foreground)
        rootPane.putClientProperty("JBR.titleBar.backgroundColor", background)
        rootPane.putClientProperty("JBR.titleBar.foregroundColor", foreground)
        rootPane.background = background
        rootPane.foreground = foreground
        rootPane.revalidate()
        rootPane.repaint()
    }
}
