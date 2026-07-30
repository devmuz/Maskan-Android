package com.maskan.mobileapp.data.util

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Tenant login credential scheme (02-data-models.md) — must match the
 * deployed `tenantLogin` Cloud Function exactly:
 *
 *   passwordSalt = 16 random bytes -> hex string (32 hex chars)
 *   passwordHash = SHA-256( passwordSalt + plaintextPassword ) -> hex string
 *
 * Concatenation order is salt-then-password, hashed as combined UTF-8 bytes.
 */
object PasswordHasher {
    private val secureRandom = SecureRandom()

    /** Unambiguous charset: excludes 0, O, 1, l, I. */
    private const val TEMP_PASSWORD_CHARSET = "abcdefghjkmnpqrstuvwxyzACDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytes.toHex()
    }

    fun hash(salt: String, plaintextPassword: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val combined = (salt + plaintextPassword).toByteArray(Charsets.UTF_8)
        return digest.digest(combined).toHex()
    }

    fun generateTempPassword(length: Int = 8): String =
        buildString {
            repeat(length) {
                append(TEMP_PASSWORD_CHARSET[secureRandom.nextInt(TEMP_PASSWORD_CHARSET.length)])
            }
        }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
