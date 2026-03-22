package com.worldtheater.archive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.worldtheater.archive.di.appModule
import com.worldtheater.archive.domain.repository.SettingsRepository
import com.worldtheater.archive.feature.note_list.screens.NoteListViewModel
import com.worldtheater.archive.nav.AppComposeNavigatorImpl
import com.worldtheater.archive.platform.system.DesktopToastHost
import com.worldtheater.archive.ui.theme.DesktopWindowThemeController
import org.koin.compose.koinInject
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import java.awt.Dimension

fun main() = application {
    if (GlobalContext.getOrNull() == null) {
        startKoin {
            modules(appModule)
        }
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "archivex",
    ) {
        DisposableEffect(window) {
            DesktopWindowThemeController.attach(window)
            onDispose {
                DesktopWindowThemeController.detach(window)
            }
        }
        val settingsRepository: SettingsRepository = koinInject()
        val noteListViewModel: NoteListViewModel = koinInject()
        LaunchedEffect(Unit) {
            window.minimumSize = Dimension(480, 480)
            noteListViewModel.getNotes()
        }
        val appComposeNavigator = remember { AppComposeNavigatorImpl() }
        Box(modifier = Modifier.fillMaxSize()) {
            MainActivityContent(
                settingsRepository = settingsRepository,
                appComposeNavigator = appComposeNavigator,
                noteListViewModel = noteListViewModel,
                showBiometricIssueDialog = false,
                onGoSettings = {},
                onDisableBiometric = {}
            )
            DesktopToastHost(modifier = Modifier.fillMaxSize())
        }
    }
}
