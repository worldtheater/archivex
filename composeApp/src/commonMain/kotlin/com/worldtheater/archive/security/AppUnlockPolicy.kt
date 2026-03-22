package com.worldtheater.archive.security

import com.worldtheater.archive.domain.repository.SettingsRepository

internal fun requestAppUnlockOrBypass(
    settingsRepository: SettingsRepository,
    onBypass: () -> Unit,
    onRequireAuth: () -> Unit
) {
    if (settingsRepository.isBiometricDisabled()) {
        onBypass()
    } else {
        onRequireAuth()
    }
}
