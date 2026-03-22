@file:Suppress("DEPRECATION")

package com.worldtheater.archive.nav

import androidx.navigation.NavGraphBuilder
import com.worldtheater.archive.feature.note_list.screens.NoteListViewModel
import com.worldtheater.archive.nav.base.AppComposeNavigator

fun NavGraphBuilder.appHomeNavigation(
    composeNavigator: AppComposeNavigator,
    viewModel: NoteListViewModel
) {
    registerHomeRoute(composeNavigator = composeNavigator, viewModel = viewModel)
    registerNewNoteRoute(composeNavigator = composeNavigator, viewModel = viewModel)
    registerNoteDetailRoute(composeNavigator = composeNavigator, viewModel = viewModel)

    registerSettingsRoute(composeNavigator = composeNavigator)
    registerBackupSettingsRoute(composeNavigator = composeNavigator, viewModel = viewModel)
    registerImportExportRoute(composeNavigator = composeNavigator, viewModel = viewModel)
    registerAboutRoute(composeNavigator = composeNavigator)
    registerDebugToolsRoute(composeNavigator = composeNavigator)
}
