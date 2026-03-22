package com.worldtheater.archive.security

import com.worldtheater.archive.domain.repository.SettingsRepository
import com.worldtheater.archive.feature.note_list.screens.NoteListViewModel
import com.worldtheater.archive.platform.data.LocalNoteRepositoryPlatform
import com.worldtheater.archive.util.log.L

internal suspend fun shouldSkipBiometricForEmptyData(
    tag: String,
    settingsRepository: SettingsRepository
): Boolean {
    if (settingsRepository.isBiometricDisabled()) return false

    return try {
        LocalNoteRepositoryPlatform.ensureDatabaseInitialized()
        LocalNoteRepositoryPlatform.noteDao().getAll().isEmpty()
    } catch (e: Exception) {
        L.e(tag, "Failed to inspect local data before auth check", e)
        false
    }
}

internal suspend fun disableBiometricAuthAndProceed(
    settingsRepository: SettingsRepository,
    onBiometricSuccess: () -> Unit
) {
    settingsRepository.setBiometricDisabled(true)
    onBiometricSuccess()
}

internal fun initSecureDbAndLoadNotesCore(
    tag: String,
    noteListViewModel: NoteListViewModel
): Throwable? {
    return try {
        LocalNoteRepositoryPlatform.ensureDatabaseInitialized()
        noteListViewModel.getNotes()
        null
    } catch (e: Exception) {
        L.e(tag, "Failed to init secure DB", e)
        e
    }
}
