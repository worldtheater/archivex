package com.worldtheater.archive.nav

import com.worldtheater.archive.nav.base.AppComposeNavigator

class AppHomeNavigationActions(
    private val navigator: AppComposeNavigator
) {
    fun toNoteDetail(noteId: Int) {
        navigator.navigate(AppHomeNavigationSpec.noteDetailRoute(noteId))
    }

    fun toNewNote(parentId: Int?) {
        navigator.navigate(AppHomeNavigationSpec.newNoteRoute(parentId))
    }

    fun toSettings() {
        navigator.navigate(AppHomeNavigationSpec.settingsRoute)
    }

    fun toBackupSettings() {
        navigator.navigate(AppHomeNavigationSpec.backupSettingsRoute)
    }

    fun toImportExportSettings() {
        navigator.navigate(AppHomeNavigationSpec.importExportSettingsRoute)
    }

    fun toAboutSettings() {
        navigator.navigate(AppHomeNavigationSpec.aboutSettingsRoute)
    }

    fun toDebugTools() {
        navigator.navigate(AppHomeNavigationSpec.debugToolsRoute)
    }

    fun up() {
        navigator.navigateUp()
    }
}
