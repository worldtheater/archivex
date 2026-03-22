package com.worldtheater.archive.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.worldtheater.archive.feature.note_list.screens.NoteDetailScreen
import com.worldtheater.archive.feature.note_list.screens.NoteListScreen
import com.worldtheater.archive.feature.note_list.screens.NoteListViewModel
import com.worldtheater.archive.feature.note_list.screens.NewNoteScreen
import com.worldtheater.archive.nav.AppRouteId.NEW_NOTE
import com.worldtheater.archive.nav.AppRouteId.NOTE_DETAIL
import com.worldtheater.archive.nav.base.AppComposeNavigator

internal fun NavGraphBuilder.registerHomeRoute(
    composeNavigator: AppComposeNavigator,
    viewModel: NoteListViewModel
) {
    val actions = AppHomeNavigationActions(composeNavigator)
    composable(
        route = AppHomeNavigationSpec.homeRoutePattern,
        exitTransition = {
            androidExitTransitionFor(AppHomeNavigationSpec.homeExitPreset(targetState.destination.route))
        },
        popEnterTransition = {
            androidEnterTransitionFor(AppHomeNavigationSpec.homePopEnterPreset(initialState.destination.route))
        },
    ) {
        NoteListScreen(
            viewModel = viewModel,
            onNoteClick = { noteId ->
                actions.toNoteDetail(noteId)
            },
            onAddNoteClick = { parentId ->
                actions.toNewNote(parentId)
            },
            onSettingsClick = {
                actions.toSettings()
            }
        )
    }
}

internal fun NavGraphBuilder.registerNewNoteRoute(
    composeNavigator: AppComposeNavigator,
    viewModel: NoteListViewModel
) {
    val actions = AppHomeNavigationActions(composeNavigator)
    composable(
        route = AppHomeNavigationSpec.newNoteRoutePattern,
        arguments = listOf(
            navArgument(AppHomeNavigationSpec.parentIdArgKey) {
                type = NavType.IntType
                defaultValue = -1
            }
        ),
        enterTransition = {
            androidEnterTransitionFor(AppHomeNavigationSpec.enterPresetFor(NEW_NOTE))
        },
        popExitTransition = {
            androidExitTransitionFor(AppHomeNavigationSpec.popExitPresetFor(NEW_NOTE))
        }
    ) { backStackEntry ->
        val parentIdArg = AppHomeNavigationSpec.parseIntArgOrDefault(
            backStackEntry.arguments?.read {
                getIntOrNull(AppHomeNavigationSpec.parentIdArgKey)
            }
        )
        NewNoteScreen(
            viewModel = viewModel,
            parentFolderId = AppHomeNavigationSpec.parseOptionalParentId(parentIdArg),
            onBack = { actions.up() }
        )
    }
}

internal fun NavGraphBuilder.registerNoteDetailRoute(
    composeNavigator: AppComposeNavigator,
    viewModel: NoteListViewModel
) {
    val actions = AppHomeNavigationActions(composeNavigator)
    composable(
        route = AppHomeNavigationSpec.noteDetailRoutePattern,
        arguments = listOf(
            navArgument(AppHomeNavigationSpec.noteIdArgKey) {
                type = NavType.IntType
            },
            navArgument(AppHomeNavigationSpec.parentIdArgKey) {
                type = NavType.IntType
                defaultValue = -1
            }
        ),
        enterTransition = {
            androidEnterTransitionFor(AppHomeNavigationSpec.enterPresetFor(NOTE_DETAIL))
        },
        popExitTransition = {
            androidExitTransitionFor(AppHomeNavigationSpec.popExitPresetFor(NOTE_DETAIL))
        }
    ) { backStackEntry ->
        val noteId = AppHomeNavigationSpec.parseIntArgOrDefault(
            backStackEntry.arguments?.read {
                getIntOrNull(AppHomeNavigationSpec.noteIdArgKey)
            }
        )
        val parentIdArg = AppHomeNavigationSpec.parseIntArgOrDefault(
            backStackEntry.arguments?.read {
                getIntOrNull(AppHomeNavigationSpec.parentIdArgKey)
            }
        )
        NoteDetailScreen(
            noteId = noteId,
            parentFolderId = AppHomeNavigationSpec.parseOptionalParentId(parentIdArg),
            viewModel = viewModel,
            onBack = { actions.up() }
        )
    }
}
