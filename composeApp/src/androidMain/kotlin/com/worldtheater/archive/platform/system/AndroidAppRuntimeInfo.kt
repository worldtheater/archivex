package com.worldtheater.archive.platform.system

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.worldtheater.archive.AppContextHolder

internal object AndroidAppRuntimeInfo {

    fun isDebuggable(): Boolean {
        val flags = AppContextHolder.appContext.applicationInfo.flags
        return (flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    fun versionName(): String = packageInfo().versionName ?: "0"

    fun versionCode(): Int {
        val packageInfo = packageInfo()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
    }

    fun isBenchmarkFlavor(): Boolean = false

    fun isProdReleaseBuild(): Boolean = !isDebuggable()

    private fun packageInfo(): PackageInfo {
        val context = AppContextHolder.appContext
        val packageManager = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(context.packageName, 0)
        }
    }
}
