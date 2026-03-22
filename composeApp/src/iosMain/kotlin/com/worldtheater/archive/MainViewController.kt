package com.worldtheater.archive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.worldtheater.archive.di.appModule
import com.worldtheater.archive.domain.repository.SettingsRepository
import com.worldtheater.archive.feature.note_list.screens.NoteListViewModel
import com.worldtheater.archive.nav.AppComposeNavigatorImpl
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    runCatching {
        startKoin {
            modules(appModule)
        }
    }

    val settingsRepository: SettingsRepository = koinInject()
    val noteListViewModel: NoteListViewModel = koinInject()
    val appComposeNavigator = remember { AppComposeNavigatorImpl() }

    LaunchedEffect(Unit) {
        noteListViewModel.getNotes()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MainActivityContent(
            settingsRepository = settingsRepository,
            appComposeNavigator = appComposeNavigator,
            noteListViewModel = noteListViewModel,
            showBiometricIssueDialog = false,
            onGoSettings = {},
            onDisableBiometric = {}
        )
    }
}
