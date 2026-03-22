package com.worldtheater.archive.platform.security

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import archivex.composeapp.generated.resources.Res
import archivex.composeapp.generated.resources.biometric_prompt_subtitle
import archivex.composeapp.generated.resources.biometric_prompt_title
import archivex.composeapp.generated.resources.msg_auth_error_fmt
import archivex.composeapp.generated.resources.msg_db_init_failed_fmt
import archivex.composeapp.generated.resources.msg_open_settings_failed
import com.worldtheater.archive.domain.repository.SettingsRepository
import com.worldtheater.archive.feature.note_list.screens.NoteListViewModel
import com.worldtheater.archive.security.initSecureDbAndLoadNotesCore
import com.worldtheater.archive.security.requestAppUnlockOrBypass
import com.worldtheater.archive.util.log.L
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

internal fun AppCompatActivity.initSecureDbAndLoadNotes(
    tag: String,
    noteListViewModel: NoteListViewModel
) {
    val error = initSecureDbAndLoadNotesCore(
        tag = tag,
        noteListViewModel = noteListViewModel
    )
    if (error != null) {
        Toast.makeText(
            this,
            s(Res.string.msg_db_init_failed_fmt, error.message ?: ""),
            Toast.LENGTH_LONG
        ).show()
        finish()
    }
}

internal fun AppCompatActivity.requestAppUnlock(
    tag: String,
    settingsRepository: SettingsRepository,
    onIssue: () -> Unit,
    onSuccess: () -> Unit
) {
    requestAppUnlockOrBypass(
        settingsRepository = settingsRepository,
        onBypass = onSuccess,
        onRequireAuth = {
            promptAppUnlockBiometric(
                tag = tag,
                title = s(Res.string.biometric_prompt_title),
                subtitle = s(Res.string.biometric_prompt_subtitle),
                onIssue = onIssue,
                onFatalError = { errString ->
                    handleBiometricFatalError(errString)
                },
                onSuccess = onSuccess
            )
        }
    )
}

private fun AppCompatActivity.promptAppUnlockBiometric(
    tag: String,
    title: String,
    subtitle: String,
    onIssue: () -> Unit,
    onFatalError: (CharSequence) -> Unit,
    onSuccess: () -> Unit
) {
    val executor = ContextCompat.getMainExecutor(this)
    val biometricPrompt = BiometricPrompt(
        this,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                L.e(tag, "Biometric authentication error code=$errorCode")

                if (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                    errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE ||
                    errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL
                ) {
                    onIssue()
                    return
                }
                onFatalError(errString)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
        }
    )

    val promptInfo = DeviceAuth.createPromptInfo(
        title = title,
        subtitle = subtitle
    )
    biometricPrompt.authenticate(promptInfo)
}

internal fun AppCompatActivity.handleBiometricIssueGoSettings() {
    val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
    try {
        startActivity(intent)
        finish()
    } catch (e: Exception) {
        Toast.makeText(
            this,
            s(Res.string.msg_open_settings_failed),
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }
}

private fun AppCompatActivity.handleBiometricFatalError(errString: CharSequence) {
    Toast.makeText(
        this,
        s(Res.string.msg_auth_error_fmt, errString),
        Toast.LENGTH_LONG
    ).show()
    finish()
}

private fun s(resource: StringResource, vararg args: Any): String {
    return runBlocking { getString(resource, *args) }
}
