package com.worldtheater.archive.platform.system

interface AppVersionInfoProvider {
    fun versionName(): String
    fun versionCode(): Int
}

interface LanguageCodeProvider {
    fun currentLanguageCode(): String
}

interface RelativeTimeFormatter {
    fun format(timeMillis: Long): String?
}

interface DateTimeFormatter {
    fun formatDateTime(timeMillis: Long): String
}

interface PlatformSystemProvider {
    val appVersionInfoProvider: AppVersionInfoProvider
    val languageCodeProvider: LanguageCodeProvider
    val relativeTimeFormatter: RelativeTimeFormatter
    val isBenchmarkFlavor: Boolean
    val isProdReleaseBuild: Boolean
    fun formatDateTime(timeMillis: Long): String
    fun showShortMessage(message: String)
}

fun defaultAppVersionInfoProvider(): AppVersionInfoProvider =
    defaultPlatformSystemProvider().appVersionInfoProvider

fun defaultLanguageCodeProvider(): LanguageCodeProvider =
    defaultPlatformSystemProvider().languageCodeProvider

fun defaultRelativeTimeFormatter(): RelativeTimeFormatter =
    defaultPlatformSystemProvider().relativeTimeFormatter

fun isBenchmarkFlavorByPlatform(): Boolean =
    defaultPlatformSystemProvider().isBenchmarkFlavor

fun isProdReleaseBuildByPlatform(): Boolean =
    defaultPlatformSystemProvider().isProdReleaseBuild

class DefaultDateTimeFormatter : DateTimeFormatter {
    override fun formatDateTime(timeMillis: Long): String =
        defaultPlatformSystemProvider().formatDateTime(timeMillis)
}

interface AppFeatureFlags {
    val guidePresetDataEnabled: Boolean
}

class DefaultAppFeatureFlags : AppFeatureFlags {
    override val guidePresetDataEnabled: Boolean
        get() = !isBenchmarkFlavorByPlatform()
}

interface UserMessageSink {
    fun showShort(message: String)
}

class DefaultUserMessageSink : UserMessageSink {
    override fun showShort(message: String) {
        defaultPlatformSystemProvider().showShortMessage(message)
    }
}
