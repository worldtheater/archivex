package com.worldtheater.archive.platform.gateway

import android.content.Intent
import android.net.Uri
import com.worldtheater.archive.AppContextHolder

actual fun openExternalUrlByPlatform(url: String): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        AppContextHolder.appContext.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}
