package com.worldtheater.archive.platform.auth

object DesktopDeviceAuthAvailabilityChecker : DeviceAuthAvailabilityChecker {
    override fun isAvailable(): Boolean = false
}

object DesktopSensitiveAuthAvailabilityProvider : SensitiveAuthAvailabilityProvider {
    override fun availability(): SensitiveAuthAvailability = SensitiveAuthAvailability.UNAVAILABLE
}

object DesktopSensitiveAuthPrompt : SensitiveAuthPrompt {
    override suspend fun authenticate(title: String, subtitle: String): Boolean = true
}
