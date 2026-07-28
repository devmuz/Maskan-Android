package com.dxbaistudio.maskan.data.util

import java.security.SecureRandom

/**
 * Property ID code generation algorithm (02-data-models.md) — must match
 * exactly so codes generated on Android are consistent with iOS/Flutter:
 *
 * 1. Take (buildingName + address), uppercase, strip to A-Z ASCII letters only.
 * 2. Take the first 5 characters of that as the prefix.
 * 3. If fewer than 5 letters were available, pad from a random filler string
 *    (5 random chars from ABCDEFGHJKMNPQRSTUVWXYZ — no I, O).
 * 4. If still short, pad with literal 'X'.
 * 5. Append 3 random digits (0-9).
 * 6. Result: an 8-character code, e.g. "WLWAP482".
 */
object PropertyIdCodeGenerator {
    private const val FILLER_CHARSET = "ABCDEFGHJKMNPQRSTUVWXYZ"
    private val secureRandom = SecureRandom()

    fun stripToLetters(input: String): String =
        input.uppercase().filter { it in 'A'..'Z' }

    fun randomFiller(length: Int = 5): String =
        buildString { repeat(length) { append(FILLER_CHARSET[secureRandom.nextInt(FILLER_CHARSET.length)]) } }

    fun randomDigits(length: Int = 3): String =
        buildString { repeat(length) { append(secureRandom.nextInt(10)) } }

    fun generate(buildingNameAndAddress: String, filler: String, digits: String): String {
        val letters = stripToLetters(buildingNameAndAddress)
        var prefix = letters.take(5)
        if (prefix.length < 5) {
            prefix += filler.take(5 - prefix.length)
        }
        if (prefix.length < 5) {
            prefix += "X".repeat(5 - prefix.length)
        }
        return prefix + digits.take(3).padEnd(3, '0')
    }

    /**
     * A fixed filler/digit pair generated once per flat entry per form
     * session, so the live preview doesn't jump around on every keystroke.
     */
    class Session(
        val filler: String = randomFiller(),
        val digits: String = randomDigits(),
    ) {
        fun derive(buildingNameAndAddress: String): String =
            generate(buildingNameAndAddress, filler, digits)
    }
}
