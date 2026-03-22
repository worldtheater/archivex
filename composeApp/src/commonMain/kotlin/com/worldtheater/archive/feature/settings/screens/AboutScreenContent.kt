package com.worldtheater.archive.feature.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldtheater.archive.feature.settings.components.*
import com.worldtheater.archive.ui.shape.SmoothRoundedCornerShape

@Composable
fun AboutScreenContent(
    listState: LazyListState,
    contentTopPadding: Dp,
    appName: String,
    versionLabel: String,
    versionSubtitle: String,
    privacyPolicyTitle: String,
    privacyPolicySubtitle: String,
    openSourceTitle: String,
    openSourceSubtitle: String,
    feedbackTitle: String,
    feedbackSubtitle: String,
    logoBgColor: Color,
    onVersionClick: (() -> Unit)?,
    onFeedbackClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onOpenSourceClick: () -> Unit,
    appLogo: @Composable () -> Unit
) {
    Scaffold(
        content = { paddingValues ->
            androidx.compose.foundation.lazy.LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = contentTopPadding,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = SmoothRoundedCornerShape(20.dp),
                            color = logoBgColor
                        ) {
                            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                appLogo()
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                item {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = SETTINGS_SECTION_HORIZONTAL_PADDING)
                            .fillMaxWidth(),
                        shape = SmoothRoundedCornerShape(radius = SETTINGS_SECTION_CARD_RADIUS),
                        color = settingsSectionCardColor()
                    ) {
                        Column {
                            SettingsItem(
                                title = versionLabel,
                                subtitle = versionSubtitle,
                                debounceClick = false,
                                onClick = onVersionClick
                            )
                            Spacer(modifier = Modifier.height(SETTINGS_SECTION_ITEM_SPACING))
                            SettingsItem(
                                title = feedbackTitle,
                                subtitle = feedbackSubtitle,
                                onClick = onFeedbackClick
                            )
                            Spacer(modifier = Modifier.height(SETTINGS_SECTION_ITEM_SPACING))
                            SettingsItem(
                                title = privacyPolicyTitle,
                                subtitle = privacyPolicySubtitle,
                                onClick = onPrivacyPolicyClick
                            )
                            Spacer(modifier = Modifier.height(SETTINGS_SECTION_ITEM_SPACING))
                            SettingsItem(
                                title = openSourceTitle,
                                subtitle = openSourceSubtitle,
                                onClick = onOpenSourceClick
                            )
                        }
                    }
                }
            }
        }
    )
}
