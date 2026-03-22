package com.worldtheater.archive.nav.base

import androidx.navigation.NavController
import androidx.navigation.navOptions

suspend fun AppComposeNavigator.handleNavigationCommands(navController: NavController) {
    navigationCommands.collect { navController.handleComposeNavigationCommand(it) }
}

private fun NavController.handleComposeNavigationCommand(navigationCommand: NavigationCommand) {
    when (navigationCommand) {
        is ComposeNavigationCommand.NavigateToRoute -> navigate(navigationCommand.route)
        is ComposeNavigationCommand.NavigateAndClearBackStack -> {
            navigate(
                navigationCommand.route,
                navOptions { popUpTo(0) }
            )
        }

        NavigationCommand.NavigateUp -> navigateUp()
        is ComposeNavigationCommand.PopUpToRoute -> {
            popBackStack(navigationCommand.route, navigationCommand.inclusive)
        }

        is ComposeNavigationCommand.NavigateUpWithResult<*> -> navUpWithResult(navigationCommand)
    }
}

private fun NavController.navUpWithResult(
    navigationCommand: ComposeNavigationCommand.NavigateUpWithResult<*>
) {
    val backStackEntry =
        navigationCommand.route?.let { getBackStackEntry(it) }
            ?: previousBackStackEntry
    backStackEntry?.savedStateHandle?.set(
        navigationCommand.key,
        navigationCommand.result
    )

    navigationCommand.route?.let {
        popBackStack(it, false)
    } ?: run {
        navigateUp()
    }
}
