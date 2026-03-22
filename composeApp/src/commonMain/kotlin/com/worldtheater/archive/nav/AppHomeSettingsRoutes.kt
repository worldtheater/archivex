package com.worldtheater.archive.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.worldtheater.archive.feature.note_list.screens.NoteListViewModel
import com.worldtheater.archive.feature.settings.screens.*
import com.worldtheater.archive.nav.AppRouteId.*
import com.worldtheater.archive.nav.base.AppComposeNavigator

internal fun NavGraphBuilder.registerSettingsRoute(
    composeNavigator: AppComposeNavigator
) {
    val actions = AppHomeNavigationActions(composeNavigator)
    composable(
        route = AppHomeNavigationSpec.settingsRoute,
        enterTransition = {
            androidEnterTransitionFor(AppHomeNavigationSpec.enterPresetFor(SETTINGS))
        },
        popExitTransition = {
            androidExitTransitionFor(AppHomeNavigationSpec.popExitPresetFor(SETTINGS))
        },
        popEnterTransition = {
            androidEnterTransitionFor(AppHomeNavigationSpec.nestedPopEnterPresetFor(SETTINGS))
        },
        exitTransition = {
            androidExitTransitionFor(AppHomeNavigationSpec.nestedExitPresetFor(SETTINGS))
        }
    ) {
        SettingsScreen(
            onBack = { actions.up() },
            onBackupSettingsClick = { actions.toBackupSettings() },
            onImportExportSettingsClick = { actions.toImportExportSettings() },
            onAboutClick = { actions.toAboutSettings() }
        )
    }
}

internal fun NavGraphBuilder.registerBackupSettingsRoute(
    composeNavigator: AppComposeNavigator,
    viewModel: NoteListViewModel
) {
    val actions = AppHomeNavigationActions(composeNavigator)
    composable(
        route = AppHomeNavigationSpec.backupSettingsRoute,
        enterTransition = {
            androidEnterTransitionFor(AppHomeNavigationSpec.enterPresetFor(BACKUP_SETTINGS))
        },
        popExitTransition = {
            androidExitTransitionFor(AppHomeNavigationSpec.popExitPresetFor(BACKUP_SETTINGS))
        }
    ) {
        BackupSettingsScreen(
            viewModel = viewModel,
            onBack = { actions.up() }
        )
    }
}

internal fun NavGraphBuilder.registerImportExportRoute(
    composeNavigator: AppComposeNavigator,
    viewModel: NoteListViewModel
) {
    val actions = AppHomeNavigationActions(composeNavigator)
    composable(
        route = AppHomeNavigationSpec.importExportSettingsRoute,
        enterTransition = {
            androidEnterTransitionFor(AppHomeNavigationSpec.enterPresetFor(IMPORT_EXPORT_SETTINGS))
        },
        popExitTransition = {
            androidExitTransitionFor(AppHomeNavigationSpec.popExitPresetFor(IMPORT_EXPORT_SETTINGS))
        }
    ) {
        ImportExportSettingsScreen(
            viewModel = viewModel,
            onBack = { actions.up() }
        )
    }
}

internal fun NavGraphBuilder.registerAboutRoute(
    composeNavigator: AppComposeNavigator
) {
    val actions = AppHomeNavigationActions(composeNavigator)
    composable(
        route = AppHomeNavigationSpec.aboutSettingsRoute,
        enterTransition = {
            androidEnterTransitionFor(AppHomeNavigationSpec.enterPresetFor(ABOUT_SETTINGS))
        },
        popExitTransition = {
            androidExitTransitionFor(AppHomeNavigationSpec.popExitPresetFor(ABOUT_SETTINGS))
        },
        exitTransition = {
            androidExitTransitionFor(AppHomeNavigationSpec.nestedExitPresetFor(ABOUT_SETTINGS))
        },
        popEnterTransition = {
            androidEnterTransitionFor(AppHomeNavigationSpec.nestedPopEnterPresetFor(ABOUT_SETTINGS))
        }
    ) {
        AboutScreen(
            onBack = { actions.up() },
            onDebugToolsClick = { actions.toDebugTools() }
        )
    }
}

internal fun NavGraphBuilder.registerDebugToolsRoute(
    composeNavigator: AppComposeNavigator
) {
    val actions = AppHomeNavigationActions(composeNavigator)
    composable(
        route = AppHomeNavigationSpec.debugToolsRoute,
        enterTransition = {
            androidEnterTransitionFor(AppHomeNavigationSpec.enterPresetFor(DEBUG_TOOLS))
        },
        popExitTransition = {
            androidExitTransitionFor(AppHomeNavigationSpec.popExitPresetFor(DEBUG_TOOLS))
        },
        exitTransition = {
            androidExitTransitionFor(AppHomeNavigationSpec.nestedExitPresetFor(DEBUG_TOOLS))
        },
        popEnterTransition = {
            androidEnterTransitionFor(AppHomeNavigationSpec.nestedPopEnterPresetFor(DEBUG_TOOLS))
        }
    ) {
        DebugToolsScreen(
            onBack = { actions.up() }
        )
    }
}
