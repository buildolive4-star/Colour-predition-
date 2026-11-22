package com.example.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#\$%"

    private val secureRandom = SecureRandom()

    data class HashedSecret(
        val saltHex: String,
        val hashHex: String
    )

    fun hashPassword(password: String): HashedSecret {
        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val hash = pbkdf2(password.toCharArray(), salt)
        return HashedSecret(
            saltHex = salt.toHex(),
            hashHex = hash.toHex()
        )
    }

    fun verifyPassword(password: String, saltHex: String, expectedHashHex: String): Boolean {
        if (saltHex.isBlank() || expectedHashHex.isBlank()) return false
        val salt = hexToBytes(saltHex) ?: return false
        val expected = hexToBytes(expectedHashHex) ?: return false
        val actual = pbkdf2(password.toCharArray(), salt)
        return MessageDigest.isEqual(actual, expected)
    }

    fun generateStrongPassword(length: Int = 12): String {
        val targetLen = length.coerceIn(10, 24)
        val chars = CharArray(targetLen) {
            PASSWORD_ALPHABET[secureRandom.nextInt(PASSWORD_ALPHABET.length)]
        }
        return String(chars)
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray? {
        if (hex.length % 2 != 0) return null
        return try {
            ByteArray(hex.length / 2) { i ->
                hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        } catch (_: NumberFormatException) {
            null
        }
    }
}
