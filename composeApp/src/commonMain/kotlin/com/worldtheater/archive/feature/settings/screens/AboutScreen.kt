package com.worldtheater.archive.feature.settings.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import archivex.composeapp.generated.resources.*
import com.worldtheater.archive.platform.gateway.ExternalLinkOpener
import com.worldtheater.archive.platform.system.UserMessageSink
import com.worldtheater.archive.platform.system.currentTimeMillis
import com.worldtheater.archive.platform.system.defaultAppVersionInfoProvider
import com.worldtheater.archive.platform.system.defaultLanguageCodeProvider
import com.worldtheater.archive.ui.theme.rememberContentTopPadding
import com.worldtheater.archive.ui.widget.SettingsAppTopBar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private const val FEEDBACK_EMAIL = "worldtheater0x01@gmail.com"

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onDebugToolsClick: (() -> Unit)? = null
) {
    val externalLinkOpener: ExternalLinkOpener = koinInject()
    val userMessageSink: UserMessageSink = koinInject()
    val appVersionInfoProvider = defaultAppVersionInfoProvider()
    val languageCodeProvider = defaultLanguageCodeProvider()
    val title = stringResource(Res.string.about_title)
    val appName = stringResource(Res.string.app_name)
    val versionLabel = stringResource(Res.string.about_version_label)
    val versionSubtitle = stringResource(
        Res.string.about_version_value_fmt,
        appVersionInfoProvider.versionName(),
        appVersionInfoProvider.versionCode()
    )
    val privacyPolicyTitle = stringResource(Res.string.pref_privacy_policy_title)
    val privacyPolicySubtitle = stringResource(Res.string.pref_privacy_policy_summary)
    val openSourceTitle = stringResource(Res.string.pref_open_source_title)
    val openSourceSubtitle = stringResource(Res.string.pref_open_source_summary)
    val feedbackTitle = stringResource(Res.string.pref_feedback_title)
    val feedbackSubtitle = stringResource(Res.string.pref_feedback_summary)
    val feedbackEmailSubject = stringResource(Res.string.pref_feedback_email_subject)
    val openFeedbackFailed = stringResource(Res.string.msg_open_feedback_failed)
    val openPrivacyPolicyFailed = stringResource(Res.string.msg_open_privacy_policy_failed)
    val openOpenSourceFailed = stringResource(Res.string.msg_open_open_source_failed)
    val privacyPolicyUrl = if (languageCodeProvider.currentLanguageCode() == "zh") {
        "https://worldtheater.github.io/archive-privacy-policy/privacy_policy_zh.html"
    } else {
        "https://worldtheater.github.io/archive-privacy-policy/privacy_policy_en.html"
    }
    val openSourceUrl = "https://worldtheater.github.io/archive-privacy-policy/open_source_licenses.html"
    val platformUiProvider: AboutScreenPlatformUiProvider = koinInject()
    val platformUi = platformUiProvider.provide(
        onDebugToolsClick = onDebugToolsClick,
        appName = appName
    )
    val listState = rememberLazyListState()
    val contentTopPadding = rememberContentTopPadding()
    var versionTapCount by remember { mutableIntStateOf(0) }
    var lastVersionTapAt by remember { mutableLongStateOf(0L) }

    Box(modifier = Modifier.fillMaxSize()) {
        AboutScreenContent(
            listState = listState,
            contentTopPadding = contentTopPadding,
            appName = appName,
            versionLabel = versionLabel,
            versionSubtitle = versionSubtitle,
            privacyPolicyTitle = privacyPolicyTitle,
            privacyPolicySubtitle = privacyPolicySubtitle,
            openSourceTitle = openSourceTitle,
            openSourceSubtitle = openSourceSubtitle,
            feedbackTitle = feedbackTitle,
            feedbackSubtitle = feedbackSubtitle,
            logoBgColor = platformUi.logoBgColor,
            onVersionClick = if (platformUi.debugToolsEnabled && onDebugToolsClick != null) {
                {
                    val now = currentTimeMillis()
                    versionTapCount =
                        if (now - lastVersionTapAt <= 1_200L) {
                            versionTapCount + 1
                        } else {
                            1
                        }
                    lastVersionTapAt = now
                    if (versionTapCount >= 5) {
                        versionTapCount = 0
                        onDebugToolsClick.invoke()
                    }
                }
            } else {
                null
            },
            onFeedbackClick = {
                val feedbackUrl = "mailto:$FEEDBACK_EMAIL?subject=${feedbackEmailSubject.encodeForUriQuery()}"
                if (!externalLinkOpener.open(feedbackUrl)) {
                    userMessageSink.showShort(openFeedbackFailed)
                }
            },
            onPrivacyPolicyClick = {
                if (!externalLinkOpener.open(privacyPolicyUrl)) {
                    userMessageSink.showShort(openPrivacyPolicyFailed)
                }
            },
            onOpenSourceClick = {
                if (!externalLinkOpener.open(openSourceUrl)) {
                    userMessageSink.showShort(openOpenSourceFailed)
                }
            },
            appLogo = { platformUi.appLogo(appName) }
        )

        SettingsAppTopBar(
            title = title,
            onBack = onBack,
            listState = listState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

private fun String.encodeForUriQuery(): String {
    return buildString(length) {
        this@encodeForUriQuery.forEach { ch ->
            when (ch) {
                ' ' -> append("%20")
                else -> append(ch)
            }
        }
    }
}
