package com.worldtheater.archive.platform.auth

object IosDeviceAuthAvailabilityChecker : DeviceAuthAvailabilityChecker {
    override fun isAvailable(): Boolean = false
}

object IosSensitiveAuthAvailabilityProvider : SensitiveAuthAvailabilityProvider {
    override fun availability(): SensitiveAuthAvailability = SensitiveAuthAvailability.UNAVAILABLE
}

object IosSensitiveAuthPrompt : SensitiveAuthPrompt {
    override suspend fun authenticate(title: String, subtitle: String): Boolean = true
}
