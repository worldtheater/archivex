package com.worldtheater.archive.nav

import com.worldtheater.archive.nav.base.AppComposeNavigator
import com.worldtheater.archive.nav.base.ComposeNavigationCommand

class AppComposeNavigatorImpl() : AppComposeNavigator() {

    override fun navigate(route: String) {
        navigationCommands.tryEmit(ComposeNavigationCommand.NavigateToRoute(route))
    }

    override fun navigateAndClearBackStack(route: String) {
        navigationCommands.tryEmit(
            ComposeNavigationCommand.NavigateAndClearBackStack(route)
        )
    }

    override fun popUpTo(route: String, inclusive: Boolean) {
        navigationCommands.tryEmit(ComposeNavigationCommand.PopUpToRoute(route, inclusive))
    }

    override fun <T> navigateBackWithResult(
        key: String,
        result: T,
        route: String?
    ) {
        navigationCommands.tryEmit(
            ComposeNavigationCommand.NavigateUpWithResult(
                key = key,
                result = result,
                route = route
            )
        )
    }
}
