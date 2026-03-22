package com.worldtheater.archive.platform.system

import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import com.worldtheater.archive.AppContextHolder
import com.worldtheater.archive.BuildConfig

@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

internal fun AppCompatActivity.applyMainWindowPolicy(
    allowScreenshots: Boolean,
    isBenchmarkLaunch: Boolean
) {
    applySecureWindowPolicy(allowScreenshots, isBenchmarkLaunch)
    WindowCompat.setDecorFitsSystemWindows(window, false)
}

internal fun AppCompatActivity.applySecureWindowPolicy(
    allowScreenshots: Boolean,
    isBenchmarkLaunch: Boolean
) {
    val secureEnabled = !allowScreenshots && !isBenchmarkLaunch
    if (secureEnabled) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

internal fun showShortMessageAndroid(message: String) {
    Toast.makeText(AppContextHolder.appContext, message, Toast.LENGTH_SHORT).show()
}
