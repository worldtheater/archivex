package com.worldtheater.archive.platform.security

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.ref.WeakReference
import kotlin.coroutines.resume

class AndroidSensitiveAuthPrompt : com.worldtheater.archive.platform.auth.SensitiveAuthPrompt {
    override suspend fun authenticate(title: String, subtitle: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            val activity = currentActivityRef?.get()
            if (activity == null) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (continuation.isActive) continuation.resume(false)
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                    }
                }
            )

            val promptInfo = DeviceAuth.createPromptInfo(
                title = title,
                subtitle = subtitle
            )
            prompt.authenticate(promptInfo)
        }

    companion object {
        @Volatile
        private var currentActivityRef: WeakReference<FragmentActivity>? = null

        fun attachActivity(activity: FragmentActivity) {
            currentActivityRef = WeakReference(activity)
        }

        fun detachActivity(activity: FragmentActivity) {
            if (currentActivityRef?.get() === activity) {
                currentActivityRef = null
            }
        }
    }
}
