package com.worldtheater.archive.platform.system

import com.worldtheater.archive.util.TimeUtils
import java.util.Date
import java.util.Locale

private object AndroidAppVersionInfoProvider : AppVersionInfoProvider {
    override fun versionName(): String = AndroidAppRuntimeInfo.versionName()
    override fun versionCode(): Int = AndroidAppRuntimeInfo.versionCode()
}

private object AndroidLanguageCodeProvider : LanguageCodeProvider {
    override fun currentLanguageCode(): String = Locale.getDefault().language
}

private object AndroidRelativeTimeFormatter : RelativeTimeFormatter {
    override fun format(timeMillis: Long): String? {
        return if (timeMillis > 0L) TimeUtils.getRelativeTime(Date(timeMillis)) else null
    }
}

private object AndroidPlatformSystemProvider : PlatformSystemProvider {
    override val appVersionInfoProvider: AppVersionInfoProvider = AndroidAppVersionInfoProvider
    override val languageCodeProvider: LanguageCodeProvider = AndroidLanguageCodeProvider
    override val relativeTimeFormatter: RelativeTimeFormatter = AndroidRelativeTimeFormatter
    override val isBenchmarkFlavor: Boolean = AndroidAppRuntimeInfo.isBenchmarkFlavor()
    override val isProdReleaseBuild: Boolean = AndroidAppRuntimeInfo.isProdReleaseBuild()

    override fun formatDateTime(timeMillis: Long): String = TimeUtils.formatDateTime(timeMillis)

    override fun showShortMessage(message: String) {
        showShortMessageAndroid(message)
    }
}

actual fun defaultPlatformSystemProvider(): PlatformSystemProvider = AndroidPlatformSystemProvider
