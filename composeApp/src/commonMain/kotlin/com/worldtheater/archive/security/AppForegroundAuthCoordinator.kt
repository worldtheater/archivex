package com.worldtheater.archive.security

import com.worldtheater.archive.domain.repository.SettingsRepository
import com.worldtheater.archive.feature.note_list.screens.NoteListViewModel
import com.worldtheater.archive.platform.system.currentTimeMillis
import kotlin.concurrent.Volatile

internal class AppForegroundAuthCoordinator(
    private val settingsRepository: SettingsRepository,
    private val noteListViewModel: NoteListViewModel,
    private val shouldSkipBiometricForEmptyData: suspend () -> Boolean,
    private val onBiometricRequired: () -> Unit,
    private val onBiometricSuccess: () -> Unit
) {
    companion object {
        @Volatile
        private var lastBackgroundTimeMs: Long = 0L

        @Volatile
        private var hasAuthenticatedSession: Boolean = false
    }

    suspend fun onStart() {
        val now = currentTimeMillis()
        val authTimeoutMs = settingsRepository.getBiometricReauthInterval().millis
        val shouldRequireAuth = !hasAuthenticatedSession ||
            authTimeoutMs == 0L ||
            now - lastBackgroundTimeMs > authTimeoutMs
        if (!shouldRequireAuth) {
            noteListViewModel.getNotes()
            return
        }

        // Invalidate current session before prompting.
        // If auth is canceled/failed, next launch must still require auth.
        hasAuthenticatedSession = false

        noteListViewModel.clearNotes()
        if (shouldSkipBiometricForEmptyData()) {
            hasAuthenticatedSession = true
            onBiometricSuccess()
        } else {
            onBiometricRequired()
        }
    }

    fun markAuthenticatedSession() {
        hasAuthenticatedSession = true
    }

    fun onStop() {
        lastBackgroundTimeMs = currentTimeMillis()
    }
}
