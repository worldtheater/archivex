package com.worldtheater.archive.platform.security

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object JvmBackupSecret {
    private const val TRANSFORMATION_GCM = "AES/GCM/NoPadding"
    private const val TRANSFORMATION_CBC = "AES/CBC/PKCS5Padding"
    private const val ALGORITHM = "AES"
    private const val CBC_IV_SIZE = 16
    private const val GCM_IV_SIZE = 12
    private const val GCM_TAG_BITS = 128

    private const val HEADER_V3 = "HUHUv3"
    private const val HEADER_V2 = "HUHUv2"
    private const val HEADER_V4_CROSS_DEVICE = "HUHUv4x"
    private const val HEADER_V4_DEVICE_BOUND = "HUHUv4d"

    private const val SALT_SIZE = 16
    private const val KEY_LENGTH = 256
    private const val PBKDF2_V2_ITERATIONS = 10000
    private const val PBKDF2_V3_ITERATIONS = 120000
    private const val PBKDF2_V4_ITERATIONS = 600000
    private const val DEK_SIZE = 32

    fun encrypt(password: String, input: ByteArray): ByteArray {
        val secureRandom = SecureRandom()
        val salt = ByteArray(SALT_SIZE)
        secureRandom.nextBytes(salt)
        val kek = deriveKeyPbkdf2V4(password, salt)
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

    fun decrypt(password: String, input: ByteArray): ByteArray {
        if (hasHeader(input, HEADER_V4_DEVICE_BOUND)) {
            throw IllegalArgumentException(
                "Device-bound backup can only be restored on original Android device"
            )
        }
        if (hasHeader(input, HEADER_V4_CROSS_DEVICE)) {
            return decryptV4CrossDevice(password, input)
        }
        if (hasHeader(input, HEADER_V3)) {
            return decryptV3(password, input)
        }
        if (hasHeader(input, HEADER_V2)) {
            return decryptV2(password, input)
        }
        return decryptV1(password, input)
    }

    private fun decryptV4CrossDevice(password: String, input: ByteArray): ByteArray {
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

        val kek = deriveKeyPbkdf2V4(password, salt)
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

    private fun decryptV3(password: String, input: ByteArray): ByteArray {
        val headerLen = HEADER_V3.toByteArray(UTF_8).size
        val minSize = headerLen + SALT_SIZE + GCM_IV_SIZE + 1
        require(input.size >= minSize) { "Invalid encrypted payload" }

        val salt = input.copyOfRange(headerLen, headerLen + SALT_SIZE)
        val iv = input.copyOfRange(headerLen + SALT_SIZE, headerLen + SALT_SIZE + GCM_IV_SIZE)
        val content = input.copyOfRange(headerLen + SALT_SIZE + GCM_IV_SIZE, input.size)

        val key = deriveKeyPbkdf2V3(password, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION_GCM)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, ALGORITHM),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )
        return cipher.doFinal(content)
    }

    private fun decryptV2(password: String, input: ByteArray): ByteArray {
        val headerLen = HEADER_V2.toByteArray(UTF_8).size
        val minSize = headerLen + SALT_SIZE + CBC_IV_SIZE + 1
        require(input.size >= minSize) { "Invalid encrypted payload" }

        val salt = input.copyOfRange(headerLen, headerLen + SALT_SIZE)
        val iv = input.copyOfRange(headerLen + SALT_SIZE, headerLen + SALT_SIZE + CBC_IV_SIZE)
        val content = input.copyOfRange(headerLen + SALT_SIZE + CBC_IV_SIZE, input.size)

        val key = deriveKeyPbkdf2V2(password, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION_CBC)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, ALGORITHM), IvParameterSpec(iv))
        return cipher.doFinal(content)
    }

    private fun decryptV1(password: String, input: ByteArray): ByteArray {
        val key = genSecretSha256(password)
        require(input.size > CBC_IV_SIZE) { "Invalid encrypted payload" }
        val iv = input.copyOfRange(0, CBC_IV_SIZE)
        val content = input.copyOfRange(CBC_IV_SIZE, input.size)
        val cipher = Cipher.getInstance(TRANSFORMATION_CBC)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, ALGORITHM), IvParameterSpec(iv))
        return cipher.doFinal(content)
    }

    private fun deriveKeyPbkdf2V4(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_V4_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun deriveKeyPbkdf2V3(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_V3_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun deriveKeyPbkdf2V2(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_V2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        return factory.generateSecret(spec).encoded
    }

    private fun genSecretSha256(input: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(UTF_8))
    }

    private fun hasHeader(input: ByteArray, header: String): Boolean {
        val bytes = header.toByteArray(UTF_8)
        return input.size > bytes.size && Arrays.equals(input.copyOfRange(0, bytes.size), bytes)
    }
}
