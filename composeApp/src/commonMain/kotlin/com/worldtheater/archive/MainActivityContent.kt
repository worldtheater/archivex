package com.worldtheater.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import archivex.composeapp.generated.resources.*
import com.worldtheater.archive.domain.AppTheme
import com.worldtheater.archive.domain.repository.SettingsRepository
import com.worldtheater.archive.feature.note_list.screens.NoteListViewModel
import com.worldtheater.archive.feature.security.BiometricIssueDialog
import com.worldtheater.archive.nav.AppHomeNavigationSpec
import com.worldtheater.archive.nav.appHomeNavigation
import com.worldtheater.archive.nav.base.AppComposeNavigator
import com.worldtheater.archive.nav.base.handleNavigationCommands
import com.worldtheater.archive.ui.theme.ApplyPlatformThemeSideEffect
import com.worldtheater.archive.ui.theme.ArchiveThemeCore
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MainActivityContent(
    settingsRepository: SettingsRepository,
    appComposeNavigator: AppComposeNavigator,
    noteListViewModel: NoteListViewModel,
    showBiometricIssueDialog: Boolean,
    onGoSettings: () -> Unit,
    onDisableBiometric: () -> Unit
) {
    val appTheme by settingsRepository.themeFlow.collectAsState()
    val isDarkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    ArchiveThemeCore(darkTheme = isDarkTheme) {
        ApplyPlatformThemeSideEffect(darkTheme = isDarkTheme)
        val navHostController = rememberNavController()
        LaunchedEffect(Unit) {
            appComposeNavigator.handleNavigationCommands(navHostController)
        }
        NavHost(
            navController = navHostController,
            startDestination = AppHomeNavigationSpec.homeRoutePattern,
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black)
        ) {
            appHomeNavigation(
                composeNavigator = appComposeNavigator,
                viewModel = noteListViewModel
            )
        }

        if (showBiometricIssueDialog) {
            BiometricIssueDialog(
                title = stringResource(Res.string.dialog_biometric_issue_title),
                description = stringResource(Res.string.dialog_biometric_issue_msg),
                goSettingsText = stringResource(Res.string.dialog_action_go_settings),
                disableBiometricText = stringResource(Res.string.dialog_action_disable_biometric),
                onGoSettings = onGoSettings,
                onDisableBiometric = onDisableBiometric
            )
        }
    }
}
