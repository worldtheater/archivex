package com.worldtheater.archive.platform.system

import com.worldtheater.archive.BuildConfig
import com.worldtheater.archive.util.TimeUtils
import java.util.Date
import java.util.Locale

private object AndroidAppVersionInfoProvider : AppVersionInfoProvider {
    override fun versionName(): String = BuildConfig.VERSION_NAME
    override fun versionCode(): Int = BuildConfig.VERSION_CODE
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
    override val isBenchmarkFlavor: Boolean = BuildConfig.FLAVOR == "benchmark"
    override val isProdReleaseBuild: Boolean =
        BuildConfig.FLAVOR == "prod" && BuildConfig.BUILD_TYPE == "release"

    override fun formatDateTime(timeMillis: Long): String = TimeUtils.formatDateTime(timeMillis)

    override fun showShortMessage(message: String) {
        showShortMessageAndroid(message)
    }
}

actual fun defaultPlatformSystemProvider(): PlatformSystemProvider = AndroidPlatformSystemProvider
