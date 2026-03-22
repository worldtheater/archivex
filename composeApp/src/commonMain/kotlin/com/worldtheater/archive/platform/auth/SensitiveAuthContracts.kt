package com.worldtheater.archive.platform.auth

enum class SensitiveAuthAvailability {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NO_CREDENTIAL,
    UNAVAILABLE
}

interface SensitiveAuthAvailabilityProvider {
    fun availability(): SensitiveAuthAvailability

    fun isAvailable(): Boolean = availability() == SensitiveAuthAvailability.AVAILABLE
}

interface DeviceAuthAvailabilityChecker {
    fun isAvailable(): Boolean
}

interface SensitiveAuthPrompt {
    suspend fun authenticate(title: String, subtitle: String): Boolean
}

interface SensitiveAuthSessionStore {
    var sensitiveNoteAuthPassedInSession: Boolean
}
