package com.worldtheater.archive.platform.security

import android.app.Application
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.worldtheater.archive.platform.auth.DeviceAuthAvailabilityChecker
import com.worldtheater.archive.platform.auth.SensitiveAuthAvailability
import com.worldtheater.archive.platform.auth.SensitiveAuthAvailabilityProvider

object DeviceAuth {
    const val AUTHENTICATORS: Int =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun availability(context: Context): SensitiveAuthAvailability {
        val code = BiometricManager.from(context).canAuthenticate(AUTHENTICATORS)
        return when (code) {
            BiometricManager.BIOMETRIC_SUCCESS -> SensitiveAuthAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> SensitiveAuthAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> SensitiveAuthAvailability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> SensitiveAuthAvailability.NO_CREDENTIAL
            else -> SensitiveAuthAvailability.UNAVAILABLE
        }
    }

    fun createPromptInfo(title: String, subtitle: String): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
    }
}

class AndroidSensitiveAuthAvailabilityProvider(
    private val app: Application
) : SensitiveAuthAvailabilityProvider {
    override fun availability(): SensitiveAuthAvailability = DeviceAuth.availability(app)
}

class AndroidDeviceAuthAvailabilityChecker(
    private val app: Application
) : DeviceAuthAvailabilityChecker {
    override fun isAvailable(): Boolean {
        return DeviceAuth.availability(app) == SensitiveAuthAvailability.AVAILABLE
    }
}
