package com.worldtheater.archive.platform.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.worldtheater.archive.AppContextHolder
import com.worldtheater.archive.util.log.L
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeyManager {

    private const val TAG = "KeyManager"
    private const val KEY_ALIAS_DB = "archive_app_db_key"
    private const val KEY_ALIAS_BACKUP_DEVICE_BIND = "archive_app_backup_device_bind_key"
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val BACKUP_PWD_FILE = "bk_pwd"

    // Storing the encrypted password in SharedPreferences or a file would be better,
    // but for simplicity and "App Lock" context, we can just generate a fixed alias key
    // or use the Keystore to wrap a random password.
    // Ideally: Generate random 32 bytes -> Encrypt with Keystore Key -> Save to Prefs.
    // On Unlock: Read Prefs -> Decrypt with Keystore Key -> Use as DB Password.

    // SIMPLIFIED APPROACH for "Offline Notepad":
    // We will assume the existence of the KeyStore entry IS the authorization.
    // But SQLCipher needs a raw byte array or string.
    // We cannot easily "export" a Keystore key.
    // So we MUST use the "Wrap/Unwrap" or "Encrypt/Decrypt" pattern.

    // Implementation:
    // 1. Generate a random 32-byte master seed.
    // 2. Encrypt it using a KeyStore-backed AES key.
    // 3. Store the encrypted seed (and IV) in SharedPreferences (using PreferenceUtils or simple file).
    // 4. When unlocking: Decrypt the seed using Keystore key.
    // 5. Use the seed as the SQLCipher passphrase.

    fun getOrCreateKey(): ByteArray {
        // Check if we have an encrypted seed saved
        // For this prototype, let's look for a file "db_secret"
        val context = AppContextHolder.appContext
        val secretFile = File(context.filesDir.absolutePath, "db_secret")

        if (!secretFile.exists()) {
            L.i(TAG, "Creating new database secret...")
            return createNewSecret(secretFile)
        }

        return try {
            L.i(TAG, "Reading existing database secret...")
            readSecret(secretFile)
        } catch (e: Exception) {
            L.e(TAG, "Failed to read existing database secret", e)
            throw IllegalStateException(
                "Unable to read existing database key. Restore app data or clear local storage.",
                e
            )
        }
    }

    private fun createNewSecret(file: File): ByteArray {
        // 1. Generate random 32-byte passphrase
        val passphrase = ByteArray(32)
        SecureRandom().nextBytes(passphrase)

        // 2. Prepare Keystore Key
        val keyStoreKey = getOrCreateKeystoreKey(KEY_ALIAS_DB)

        // 3. Encrypt Passphrase
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyStoreKey)
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(passphrase)

        // 4. Save IV + EncryptedBytes to file
        // Format: [IV_LENGTH (1 byte)] [IV] [Encrypted Content]
        val output = ByteArrayOutputStream()
        output.write(iv.size)
        output.write(iv)
        output.write(encryptedBytes)
        file.writeBytes(output.toByteArray())

        return passphrase
    }

    private fun readSecret(file: File): ByteArray {
        val input = file.readBytes()
        val ivSize = input[0].toInt()
        val iv = input.copyOfRange(1, 1 + ivSize)
        val encryptedBytes = input.copyOfRange(1 + ivSize, input.size)

        val keyStoreKey = getOrCreateKeystoreKey(KEY_ALIAS_DB)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, keyStoreKey, spec)

        return cipher.doFinal(encryptedBytes)
    }

    private fun getOrCreateKeystoreKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)

        if (keyStore.containsAlias(alias)) {
            return (keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
        }

        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encryptDeviceBound(input: ByteArray): ByteArray {
        val key = getOrCreateKeystoreKey(KEY_ALIAS_BACKUP_DEVICE_BIND)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(input)
        val output = ByteArrayOutputStream()
        output.write(iv.size)
        output.write(iv)
        output.write(encrypted)
        return output.toByteArray()
    }

    fun decryptDeviceBound(input: ByteArray): ByteArray {
        require(input.isNotEmpty()) { "Invalid device-bound payload" }
        val ivSize = input[0].toInt()
        require(ivSize > 0) { "Invalid device-bound payload" }
        require(input.size > 1 + ivSize) { "Invalid device-bound payload" }
        val iv = input.copyOfRange(1, 1 + ivSize)
        val encrypted = input.copyOfRange(1 + ivSize, input.size)
        val key = getOrCreateKeystoreKey(KEY_ALIAS_BACKUP_DEVICE_BIND)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    // --- Backup Password Support ---

    fun saveBackupPassword(password: String) {
        // Security policy: backup passwords must not be persisted locally.
        L.w(TAG, "saveBackupPassword is disabled by security policy")
        clearLegacyBackupPassword()
    }

    fun getBackupPassword(): String? {
        // Security policy: backup passwords are never retrievable from local storage.
        clearLegacyBackupPassword()
        return null
    }

    fun clearLegacyBackupPassword() {
        val context = AppContextHolder.appContext
        val file = File(context.filesDir, BACKUP_PWD_FILE)
        if (!file.exists()) return
        runCatching { file.delete() }
            .onFailure { e -> L.e(TAG, "Failed to delete legacy backup password file", e) }
    }
}
