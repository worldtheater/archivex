package com.worldtheater.archive.platform.system

import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

private object IosAppVersionInfoProvider : AppVersionInfoProvider {
    override fun versionName(): String {
        return (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String)
            ?: "1.0"
    }

    override fun versionCode(): Int {
        return (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String)
            ?.toIntOrNull()
            ?: 1
    }
}

private object IosLanguageCodeProvider : LanguageCodeProvider {
    override fun currentLanguageCode(): String {
        val preferred = NSBundle.mainBundle.preferredLocalizations.firstOrNull() as? String
        return preferred?.substringBefore('-') ?: "en"
    }
}

private object IosRelativeTimeFormatter : RelativeTimeFormatter {
    override fun format(timeMillis: Long): String? {
        val deltaSec = (currentTimeMillis() - timeMillis) / 1000
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

private object IosPlatformSystemProvider : PlatformSystemProvider {
    override val appVersionInfoProvider: AppVersionInfoProvider = IosAppVersionInfoProvider
    override val languageCodeProvider: LanguageCodeProvider = IosLanguageCodeProvider
    override val relativeTimeFormatter: RelativeTimeFormatter = IosRelativeTimeFormatter
    override val isBenchmarkFlavor: Boolean = false
    override val isProdReleaseBuild: Boolean = false

    override fun formatDateTime(timeMillis: Long): String {
        val formatter = NSDateFormatter()
        formatter.dateFormat = "MM/dd/yyyy HH:mm"
        val unixSeconds = timeMillis.toDouble() / 1000.0
        val referenceDateSeconds = unixSeconds - 978_307_200.0
        val date = NSDate(timeIntervalSinceReferenceDate = referenceDateSeconds)
        return formatter.stringFromDate(date)
    }

    override fun showShortMessage(message: String) {
        // Avoid Objective-C variadic logging bridge crashes on iOS.
        println("[Archive] $message")
    }
}

actual fun defaultPlatformSystemProvider(): PlatformSystemProvider = IosPlatformSystemProvider
