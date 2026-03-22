package com.worldtheater.archive.nav

@Suppress("ConvertObjectToDataObject")
sealed class AppScreens(
    val route: String,
    val name: String = route
) {
    companion object {
        const val KEY_NOTE_ID = "note_id"
        const val KEY_PARENT_ID = "parent_id"
    }

    // home screen
    object AppHomeScreen : AppScreens(
        route = "home"
    ) {
        fun createRoute() = name
    }

    object NewNoteScreen : AppScreens(
        route = "new_note_screen",
        name = "new_note_screen?$KEY_PARENT_ID={$KEY_PARENT_ID}"
    ) {
        fun createRoute(parentId: Int? = null): String {
            return name.replace("{$KEY_PARENT_ID}", "${parentId ?: -1}")
        }
    }

    object NoteDetailScreen : AppScreens(
        route = "note_detail",
        name = "note_detail/{$KEY_NOTE_ID}?$KEY_PARENT_ID={$KEY_PARENT_ID}"
    ) {
        fun createRoute(noteId: Int, parentId: Int? = null): String {
            return name
                .replace("{$KEY_NOTE_ID}", "$noteId")
                .replace("{$KEY_PARENT_ID}", "${parentId ?: -1}")
        }
    }

    object AppSettingsScreen : AppScreens(
        route = "settings",
    ) {
        fun createRoute() = name
    }

    object BackupSettingsScreen : AppScreens(
        route = "backup_settings",
    ) {
        fun createRoute() = name
    }

    object ImportExportSettingsScreen : AppScreens(
        route = "import_export_settings",
    ) {
        fun createRoute() = name
    }

    object AboutSettingsScreen : AppScreens(
        route = "about_settings",
    ) {
        fun createRoute() = name
    }

    object DebugToolsScreen : AppScreens(
        route = "debug_tools",
    ) {
        fun createRoute() = name
    }

}
