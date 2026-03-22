package com.worldtheater.archive.platform.system

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private object JvmAppVersionInfoProvider : AppVersionInfoProvider {
    override fun versionName(): String = "1.0"
    override fun versionCode(): Int = 1
}

private object JvmLanguageCodeProvider : LanguageCodeProvider {
    override fun currentLanguageCode(): String = Locale.getDefault().language
}

private object JvmRelativeTimeFormatter : RelativeTimeFormatter {
    override fun format(timeMillis: Long): String? {
        val now = System.currentTimeMillis()
        val deltaSec = (now - timeMillis) / 1000
        if (deltaSec < 0) return null
        return when {
            deltaSec < 60 -> "just now"
            deltaSec < 3600 -> "${deltaSec / 60}m ago"
            deltaSec < 86400 -> "${deltaSec / 3600}h ago"
            deltaSec < 86400 * 7 -> "${deltaSec / 86400}d ago"
            else -> null
        }
    }
}

private object JvmPlatformSystemProvider : PlatformSystemProvider {
    override val appVersionInfoProvider: AppVersionInfoProvider = JvmAppVersionInfoProvider
    override val languageCodeProvider: LanguageCodeProvider = JvmLanguageCodeProvider
    override val relativeTimeFormatter: RelativeTimeFormatter = JvmRelativeTimeFormatter
    override val isBenchmarkFlavor: Boolean = false
    override val isProdReleaseBuild: Boolean = false

    override fun formatDateTime(timeMillis: Long): String {
        val locale = Locale.getDefault()
        val isSimplifiedZh = locale.language == "zh" &&
            (locale.country == "CN" || locale.toLanguageTag().contains("Hans"))
        val pattern = if (isSimplifiedZh) "yyyy/MM/dd HH:mm" else "MM/dd/yyyy HH:mm"
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        return formatter.format(Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault()))
    }

    override fun showShortMessage(message: String) {
        DesktopToastManager.show(message)
        println("[Archive] $message")
    }
}

actual fun defaultPlatformSystemProvider(): PlatformSystemProvider = JvmPlatformSystemProvider
