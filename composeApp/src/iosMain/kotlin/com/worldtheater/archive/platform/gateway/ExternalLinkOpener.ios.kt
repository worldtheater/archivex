package com.worldtheater.archive.platform.gateway

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openExternalUrlByPlatform(url: String): Boolean {
    val nsUrl = NSURL.URLWithString(url) ?: return false
    val app = UIApplication.sharedApplication
    if (!app.canOpenURL(nsUrl)) return false
    app.openURL(nsUrl)
    return true
}
