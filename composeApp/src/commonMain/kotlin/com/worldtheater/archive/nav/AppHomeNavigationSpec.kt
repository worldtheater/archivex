package com.worldtheater.archive.nav

enum class AppRouteId {
    HOME,
    NEW_NOTE,
    NOTE_DETAIL,
    SETTINGS,
    BACKUP_SETTINGS,
    IMPORT_EXPORT_SETTINGS,
    ABOUT_SETTINGS,
    DEBUG_TOOLS,
    UNKNOWN
}

enum class AppNavAnimationPreset {
    SLIDE_IN_FROM_RIGHT,
    SLIDE_OUT_TO_RIGHT,
    SLIDE_IN_FROM_BOTTOM,
    SLIDE_OUT_TO_BOTTOM,
    FADE_SCALE_IN,
    FADE_SCALE_OUT,
    SLIDE_LEFT_FADE_IN,
    SLIDE_LEFT_FADE_OUT
}

object AppHomeNavigationSpec {
    val homeRoutePattern: String = AppScreens.AppHomeScreen.name
    val newNoteRoutePattern: String = AppScreens.NewNoteScreen.name
    val noteDetailRoutePattern: String = AppScreens.NoteDetailScreen.name
    val settingsRoute: String = AppScreens.AppSettingsScreen.name
    val backupSettingsRoute: String = AppScreens.BackupSettingsScreen.name
    val importExportSettingsRoute: String = AppScreens.ImportExportSettingsScreen.name
    val aboutSettingsRoute: String = AppScreens.AboutSettingsScreen.name
    val debugToolsRoute: String = AppScreens.DebugToolsScreen.name
    const val noteIdArgKey: String = AppScreens.KEY_NOTE_ID
    const val parentIdArgKey: String = AppScreens.KEY_PARENT_ID

    fun noteDetailRoute(noteId: Int): String = AppScreens.NoteDetailScreen.createRoute(noteId)
    fun newNoteRoute(parentId: Int?): String = AppScreens.NewNoteScreen.createRoute(parentId)

    fun routeId(route: String?): AppRouteId {
        if (route == null) return AppRouteId.UNKNOWN
        return when {
            route == AppScreens.AppHomeScreen.route || route == AppScreens.AppHomeScreen.name -> AppRouteId.HOME
            route.startsWith(AppScreens.NewNoteScreen.route) -> AppRouteId.NEW_NOTE
            route.startsWith(AppScreens.NoteDetailScreen.route) -> AppRouteId.NOTE_DETAIL
            route == AppScreens.AppSettingsScreen.route || route == AppScreens.AppSettingsScreen.name -> AppRouteId.SETTINGS
            route == AppScreens.BackupSettingsScreen.route || route == AppScreens.BackupSettingsScreen.name -> AppRouteId.BACKUP_SETTINGS
            route == AppScreens.ImportExportSettingsScreen.route || route == AppScreens.ImportExportSettingsScreen.name -> AppRouteId.IMPORT_EXPORT_SETTINGS
            route == AppScreens.AboutSettingsScreen.route || route == AppScreens.AboutSettingsScreen.name -> AppRouteId.ABOUT_SETTINGS
            route == AppScreens.DebugToolsScreen.route || route == AppScreens.DebugToolsScreen.name -> AppRouteId.DEBUG_TOOLS
            else -> AppRouteId.UNKNOWN
        }
    }

    fun homeExitPreset(targetRoute: String?): AppNavAnimationPreset {
        return when (routeId(targetRoute)) {
            AppRouteId.SETTINGS,
            AppRouteId.NEW_NOTE -> AppNavAnimationPreset.FADE_SCALE_OUT

            else -> AppNavAnimationPreset.SLIDE_LEFT_FADE_OUT
        }
    }

    fun homePopEnterPreset(initialRoute: String?): AppNavAnimationPreset {
        return when (routeId(initialRoute)) {
            AppRouteId.SETTINGS,
            AppRouteId.NEW_NOTE -> AppNavAnimationPreset.FADE_SCALE_IN

            else -> AppNavAnimationPreset.SLIDE_LEFT_FADE_IN
        }
    }

    fun enterPresetFor(routeId: AppRouteId): AppNavAnimationPreset {
        return when (routeId) {
            AppRouteId.NEW_NOTE,
            AppRouteId.SETTINGS -> AppNavAnimationPreset.SLIDE_IN_FROM_BOTTOM

            AppRouteId.NOTE_DETAIL,
            AppRouteId.BACKUP_SETTINGS,
            AppRouteId.IMPORT_EXPORT_SETTINGS,
            AppRouteId.ABOUT_SETTINGS,
            AppRouteId.DEBUG_TOOLS -> AppNavAnimationPreset.SLIDE_IN_FROM_RIGHT

            else -> AppNavAnimationPreset.SLIDE_LEFT_FADE_IN
        }
    }

    fun popExitPresetFor(routeId: AppRouteId): AppNavAnimationPreset {
        return when (routeId) {
            AppRouteId.NEW_NOTE,
            AppRouteId.SETTINGS -> AppNavAnimationPreset.SLIDE_OUT_TO_BOTTOM

            AppRouteId.NOTE_DETAIL,
            AppRouteId.BACKUP_SETTINGS,
            AppRouteId.IMPORT_EXPORT_SETTINGS,
            AppRouteId.ABOUT_SETTINGS,
            AppRouteId.DEBUG_TOOLS -> AppNavAnimationPreset.SLIDE_OUT_TO_RIGHT

            else -> AppNavAnimationPreset.SLIDE_LEFT_FADE_OUT
        }
    }

    fun nestedExitPresetFor(routeId: AppRouteId): AppNavAnimationPreset {
        return when (routeId) {
            AppRouteId.SETTINGS,
            AppRouteId.ABOUT_SETTINGS,
            AppRouteId.DEBUG_TOOLS -> AppNavAnimationPreset.SLIDE_LEFT_FADE_OUT

            else -> AppNavAnimationPreset.FADE_SCALE_OUT
        }
    }

    fun nestedPopEnterPresetFor(routeId: AppRouteId): AppNavAnimationPreset {
        return when (routeId) {
            AppRouteId.SETTINGS,
            AppRouteId.ABOUT_SETTINGS,
            AppRouteId.DEBUG_TOOLS -> AppNavAnimationPreset.SLIDE_LEFT_FADE_IN

            else -> AppNavAnimationPreset.FADE_SCALE_IN
        }
    }

    fun parseIntArgOrDefault(raw: Int?, defaultValue: Int = -1): Int = raw ?: defaultValue
    fun parseOptionalParentId(rawParentId: Int?): Int? = rawParentId?.takeIf { it >= 0 }
}
