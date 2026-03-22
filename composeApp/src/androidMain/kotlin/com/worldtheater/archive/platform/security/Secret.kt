package com.worldtheater.archive.platform.security

import com.worldtheater.archive.BuildConfig
import com.worldtheater.archive.domain.BackupSecurityMode
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Secret {

    private const val TRANSFORMATION_GCM = "AES/GCM/NoPadding"
    private const val TRANSFORMATION_CBC = "AES/CBC/PKCS5Padding"
    private const val ALGORITHM = "AES"
    private const val CBC_IV_SIZE = 16
    private const val GCM_IV_SIZE = 12
    private const val GCM_TAG_BITS = 128

    // V3 Constants (AEAD)
    private const val HEADER_V3 = "HUHUv3"
    private const val PBKDF2_V3_ITERATIONS = 120000
    private const val PBKDF2_V4_ITERATIONS = 600000
    private const val HEADER_V4_CROSS_DEVICE = "HUHUv4x"
    private const val HEADER_V4_DEVICE_BOUND = "HUHUv4d"
    private const val DEK_SIZE = 32

    // V2 Constants
    private const val HEADER_V2 = "HUHUv2"
    private const val SALT_SIZE = 16
    private const val PBKDF2_V2_ITERATIONS = 10000
    private const val KEY_LENGTH = 256

    fun encryptBackup(
        alias: String,
        input: ByteArray,
        mode: BackupSecurityMode
    ): ByteArray {
        return when (mode) {
            BackupSecurityMode.CROSS_DEVICE -> encryptV4CrossDevice(alias, input)
            BackupSecurityMode.DEVICE_BOUND -> encryptV4DeviceBound(alias, input)
        }
    }

    fun encrypt(alias: String, input: ByteArray): ByteArray {
        // Legacy default API keeps cross-device compatibility.
        return encryptV4CrossDevice(alias, input)
    }

    private fun encryptV4CrossDevice(alias: String, input: ByteArray): ByteArray {
        val secureRandom = SecureRandom()
        val salt = ByteArray(SALT_SIZE)
        secureRandom.nextBytes(salt)
        val kek = deriveKeyPbkdf2V4(alias, salt)
        val dek = ByteArray(DEK_SIZE)
        secureRandom.nextBytes(dek)

        val wrapIv = ByteArray(GCM_IV_SIZE)
        secureRandom.nextBytes(wrapIv)
        val wrapCipher = Cipher.getInstance(TRANSFORMATION_GCM)
        wrapCipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(kek, ALGORITHM),
            GCMParameterSpec(GCM_TAG_BITS, wrapIv)
        )
        val wrappedDek = wrapCipher.doFinal(dek)

        val dataIv = ByteArray(GCM_IV_SIZE)
        secureRandom.nextBytes(dataIv)
        val dataCipher = Cipher.getInstance(TRANSFORMATION_GCM)
        dataCipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(dek, ALGORITHM),
            GCMParameterSpec(GCM_TAG_BITS, dataIv)
        )
        val encryptedBytes = dataCipher.doFinal(input)

        val headerBytes = HEADER_V4_CROSS_DEVICE.toByteArray(UTF_8)
        val wrappedLen = wrappedDek.size
        require(wrappedLen <= 0xFFFF) { "Wrapped DEK too large" }

        // Format: Header + Salt + WrapIV + WrappedDEKLen(2) + WrappedDEK + DataIV + Content
        return headerBytes +
                salt +
                wrapIv +
                byteArrayOf(
                    ((wrappedLen ushr 8) and 0xFF).toByte(),
                    (wrappedLen and 0xFF).toByte()
                ) +
                wrappedDek +
                dataIv +
                encryptedBytes
    }

    private fun encryptV4DeviceBound(alias: String, input: ByteArray): ByteArray {
        val secureRandom = SecureRandom()
        val salt = ByteArray(SALT_SIZE)
        secureRandom.nextBytes(salt)
        val kek = deriveKeyPbkdf2V4(alias, salt)
        val dek = ByteArray(DEK_SIZE)
        secureRandom.nextBytes(dek)

        val wrapIv = ByteArray(GCM_IV_SIZE)
        secureRandom.nextBytes(wrapIv)
        val wrapCipher = Cipher.getInstance(TRANSFORMATION_GCM)
        wrapCipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(kek, ALGORITHM),
            GCMParameterSpec(GCM_TAG_BITS, wrapIv)
        )
        val wrappedDek = wrapCipher.doFinal(dek)
        val deviceWrappedDek = KeyManager.encryptDeviceBound(wrappedDek)

        val dataIv = ByteArray(GCM_IV_SIZE)
        secureRandom.nextBytes(dataIv)
        val dataCipher = Cipher.getInstance(TRANSFORMATION_GCM)
        dataCipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(dek, ALGORITHM),
            GCMParameterSpec(GCM_TAG_BITS, dataIv)
        )
        val encryptedBytes = dataCipher.doFinal(input)

        val headerBytes = HEADER_V4_DEVICE_BOUND.toByteArray(UTF_8)
        val wrappedLen = wrappedDek.size
        val deviceWrappedLen = deviceWrappedDek.size
        require(wrappedLen <= 0xFFFF) { "Wrapped DEK too large" }
        require(deviceWrappedLen <= 0xFFFF) { "Device wrapped key too large" }

        // Format:
        // Header + Salt + WrapIV + WrappedDEKLen(2) + WrappedDEK + DeviceWrappedLen(2) +
        // DeviceWrappedDEK + DataIV + Content
        return headerBytes +
                salt +
                wrapIv +
                byteArrayOf(
                    ((wrappedLen ushr 8) and 0xFF).toByte(),
                    (wrappedLen and 0xFF).toByte()
                ) +
                wrappedDek +
                byteArrayOf(
                    ((deviceWrappedLen ushr 8) and 0xFF).toByte(),
                    (deviceWrappedLen and 0xFF).toByte()
                ) +
                deviceWrappedDek +
                dataIv +
                encryptedBytes
    }

    fun decrypt(alias: String, input: ByteArray): ByteArray {
        if (hasHeader(input, HEADER_V4_DEVICE_BOUND)) {
            return decryptV4DeviceBound(alias, input)
        }
        if (hasHeader(input, HEADER_V4_CROSS_DEVICE)) {
            return decryptV4CrossDevice(alias, input)
        }
        if (hasHeader(input, HEADER_V3)) {
            return decryptV3(alias, input)
        }
        if (isProdReleaseBuild()) {
            throw IllegalArgumentException("Legacy backup format is disabled in prod release builds")
        }

        val headerBytes = HEADER_V2.toByteArray(UTF_8)

        // Check for V2 Header
        if (input.size > headerBytes.size &&
            Arrays.equals(input.copyOfRange(0, headerBytes.size), headerBytes)
        ) {
            return decryptV2(alias, input)
        }

        // Fallback to V1
        return decryptV1(alias, input)
    }

    private fun decryptV4CrossDevice(alias: String, input: ByteArray): ByteArray {
        val headerLen = HEADER_V4_CROSS_DEVICE.toByteArray(UTF_8).size
        val minSize = headerLen + SALT_SIZE + GCM_IV_SIZE + 2 + 1 + GCM_IV_SIZE + 1
        require(input.size >= minSize) { "Invalid encrypted payload" }

        var offset = headerLen
        val salt = input.copyOfRange(offset, offset + SALT_SIZE)
        offset += SALT_SIZE

        val wrapIv = input.copyOfRange(offset, offset + GCM_IV_SIZE)
        offset += GCM_IV_SIZE

        val wrappedLen =
            ((input[offset].toInt() and 0xFF) shl 8) or (input[offset + 1].toInt() and 0xFF)
        offset += 2
        require(wrappedLen > 0) { "Invalid wrapped key length" }
        require(offset + wrappedLen + GCM_IV_SIZE <= input.size) { "Invalid encrypted payload" }

        val wrappedDek = input.copyOfRange(offset, offset + wrappedLen)
        offset += wrappedLen

        val dataIv = input.copyOfRange(offset, offset + GCM_IV_SIZE)
        offset += GCM_IV_SIZE
        val content = input.copyOfRange(offset, input.size)

        val kek = deriveKeyPbkdf2V4(alias, salt)
        val unwrapCipher = Cipher.getInstance(TRANSFORMATION_GCM)
        unwrapCipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(kek, ALGORITHM),
            GCMParameterSpec(GCM_TAG_BITS, wrapIv)
        )
        val dek = unwrapCipher.doFinal(wrappedDek)
        require(dek.size == DEK_SIZE) { "Invalid decrypted key size" }

        val dataCipher = Cipher.getInstance(TRANSFORMATION_GCM)
        dataCipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(dek, ALGORITHM),
            GCMParameterSpec(GCM_TAG_BITS, dataIv)
        )
        return dataCipher.doFinal(content)
    }

    private fun decryptV4DeviceBound(alias: String, input: ByteArray): ByteArray {
        val headerLen = HEADER_V4_DEVICE_BOUND.toByteArray(UTF_8).size
        val minSize = headerLen + SALT_SIZE + GCM_IV_SIZE + 2 + 1 + 2 + 1 + GCM_IV_SIZE + 1
        require(input.size >= minSize) { "Invalid encrypted payload" }

        var offset = headerLen
        val salt = input.copyOfRange(offset, offset + SALT_SIZE)
        offset += SALT_SIZE

        val wrapIv = input.copyOfRange(offset, offset + GCM_IV_SIZE)
        offset += GCM_IV_SIZE

        val wrappedLen =
            ((input[offset].toInt() and 0xFF) shl 8) or (input[offset + 1].toInt() and 0xFF)
        offset += 2
        require(wrappedLen > 0) { "Invalid wrapped key length" }
        require(offset + wrappedLen + 2 + GCM_IV_SIZE <= input.size) { "Invalid encrypted payload" }
        val wrappedDek = input.copyOfRange(offset, offset + wrappedLen)
        offset += wrappedLen

        val deviceWrappedLen =
            ((input[offset].toInt() and 0xFF) shl 8) or (input[offset + 1].toInt() and 0xFF)
        offset += 2
        require(deviceWrappedLen > 0) { "Invalid device-bound key length" }
        require(offset + deviceWrappedLen + GCM_IV_SIZE <= input.size) { "Invalid encrypted payload" }
        val deviceWrappedDek = input.copyOfRange(offset, offset + deviceWrappedLen)
        offset += deviceWrappedLen

        val dataIv = input.copyOfRange(offset, offset + GCM_IV_SIZE)
        offset += GCM_IV_SIZE
        val content = input.copyOfRange(offset, input.size)

        val wrappedDekFromDevice = try {
            KeyManager.decryptDeviceBound(deviceWrappedDek)
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Device-bound backup can only be restored on original device",
                e
            )
        }
        require(wrappedDekFromDevice.contentEquals(wrappedDek)) { "Device-bound key mismatch" }

        val kek = deriveKeyPbkdf2V4(alias, salt)
        val unwrapCipher = Cipher.getInstance(TRANSFORMATION_GCM)
        unwrapCipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(kek, ALGORITHM),
            GCMParameterSpec(GCM_TAG_BITS, wrapIv)
        )
        val dek = unwrapCipher.doFinal(wrappedDek)
        require(dek.size == DEK_SIZE) { "Invalid decrypted key size" }

        val dataCipher = Cipher.getInstance(TRANSFORMATION_GCM)
        dataCipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(dek, ALGORITHM),
            GCMParameterSpec(GCM_TAG_BITS, dataIv)
        )
        return dataCipher.doFinal(content)
    }

    private fun decryptV3(alias: String, input: ByteArray): ByteArray {
        val headerLen = HEADER_V3.toByteArray(UTF_8).size
        val minSize = headerLen + SALT_SIZE + GCM_IV_SIZE + 1
        require(input.size >= minSize) { "Invalid encrypted payload" }

        val salt = input.copyOfRange(headerLen, headerLen + SALT_SIZE)
        val iv = input.copyOfRange(headerLen + SALT_SIZE, headerLen + SALT_SIZE + GCM_IV_SIZE)
        val content = input.copyOfRange(headerLen + SALT_SIZE + GCM_IV_SIZE, input.size)

        val key = deriveKeyPbkdf2V3(alias, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION_GCM)
        val keySpec = SecretKeySpec(key, ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        return cipher.doFinal(content)
    }

    private fun decryptV2(alias: String, input: ByteArray): ByteArray {
        val headerLen = HEADER_V2.toByteArray(UTF_8).size
        // Parse: Header | Salt | IV | Content
        val minSize = headerLen + SALT_SIZE + CBC_IV_SIZE + 1
        require(input.size >= minSize) { "Invalid encrypted payload" }

        val salt = input.copyOfRange(headerLen, headerLen + SALT_SIZE)
        val iv = input.copyOfRange(headerLen + SALT_SIZE, headerLen + SALT_SIZE + CBC_IV_SIZE)
        val content = input.copyOfRange(headerLen + SALT_SIZE + CBC_IV_SIZE, input.size)

        val key = deriveKeyPbkdf2V2(alias, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION_CBC)
        val keySpec = SecretKeySpec(key, ALGORITHM)
        val ivParams = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParams)
        return cipher.doFinal(content)
    }

    private fun decryptV1(alias: String, input: ByteArray): ByteArray {
        val key = genSecretSha256(alias) // Legacy Key Gen
        require(input.size > CBC_IV_SIZE) { "Invalid encrypted payload" }
        val iv = ByteArray(CBC_IV_SIZE)
        val data = ByteArray(input.size - CBC_IV_SIZE)
        System.arraycopy(input, 0, iv, 0, CBC_IV_SIZE)
        System.arraycopy(input, CBC_IV_SIZE, data, 0, data.size)

        val cipher = Cipher.getInstance(TRANSFORMATION_CBC)
        val keySpec = SecretKeySpec(key, ALGORITHM)
        val params = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, params)
        return cipher.doFinal(data)
    }

    private fun deriveKeyPbkdf2V3(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_V3_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun deriveKeyPbkdf2V4(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_V4_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun deriveKeyPbkdf2V2(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_V2_ITERATIONS, KEY_LENGTH)
        // Keep legacy derivation for existing v2 backup compatibility.
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        return factory.generateSecret(spec).encoded
    }

    private fun hasHeader(input: ByteArray, header: String): Boolean {
        val bytes = header.toByteArray(UTF_8)
        return input.size > bytes.size && Arrays.equals(input.copyOfRange(0, bytes.size), bytes)
    }

    private fun isProdReleaseBuild(): Boolean {
        return BuildConfig.FLAVOR == "prod" && BuildConfig.BUILD_TYPE == "release"
    }

    private fun genSecretSha256(input: String): ByteArray {
        val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(UTF_8))
    }
}
