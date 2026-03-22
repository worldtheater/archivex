package com.worldtheater.archive.platform.gateway

import java.awt.Desktop
import java.net.URI

actual fun openExternalUrlByPlatform(url: String): Boolean {
    return try {
        if (!Desktop.isDesktopSupported()) return false
        val desktop = Desktop.getDesktop()
        if (!desktop.isSupported(Desktop.Action.BROWSE)) return false
        desktop.browse(URI(url))
        true
    } catch (_: Exception) {
        false
    }
}
