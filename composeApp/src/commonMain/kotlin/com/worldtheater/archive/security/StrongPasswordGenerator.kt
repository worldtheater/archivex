package com.worldtheater.archive.security

import com.worldtheater.archive.platform.security.secureRandomIntPlatform


const val MIN_GENERATED_PASSWORD_LENGTH = 8
const val MAX_GENERATED_PASSWORD_LENGTH = 20
const val DEFAULT_GENERATED_PASSWORD_LENGTH = 12
const val PASSWORD_STRENGTH_MAX_SCORE = 6
private const val COMMON_WORD_PENALTY = 2
private const val PATTERN_PENALTY = 1
private const val MAX_TOTAL_PATTERN_PENALTY = 3

enum class PasswordStrengthLevel {
    VERY_WEAK,
    WEAK,
    FAIR,
    GOOD,
    STRONG
}

data class PasswordStrengthResult(
    val score: Int,
    val level: PasswordStrengthLevel
)

fun evaluateGeneratedPasswordStrength(password: String): PasswordStrengthResult {
    if (password.isBlank()) {
        return PasswordStrengthResult(
            score = 0,
            level = PasswordStrengthLevel.VERY_WEAK
        )
    }

    var score = 0
    if (password.length >= 8) score += 1
    if (password.length >= 12) score += 1
    if (password.length >= 16) score += 1

    val hasLower = password.any { it.isLowerCase() }
    val hasUpper = password.any { it.isUpperCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSymbol = password.any { !it.isLetterOrDigit() }
    val classCount = listOf(hasLower, hasUpper, hasDigit, hasSymbol).count { it }
    if (classCount >= 2) score += 1
    if (classCount >= 3) score += 1
    if (classCount >= 4) score += 1

    val normalized = password.lowercase()
    var penalty = 0

    if (password.toSet().size == 1) {
        penalty += COMMON_WORD_PENALTY
    }
    if (containsCommonWord(normalized)) {
        penalty += COMMON_WORD_PENALTY
    }
    if (hasLongSequentialRun(normalized)) {
        penalty += PATTERN_PENALTY
    }
    if (hasKeyboardPattern(normalized)) {
        penalty += PATTERN_PENALTY
    }
    if (hasRepeatingChunk(normalized)) {
        penalty += PATTERN_PENALTY
    }
    if (looksLikeDateOrYear(normalized)) {
        penalty += PATTERN_PENALTY
    }
    score -= penalty.coerceAtMost(MAX_TOTAL_PATTERN_PENALTY)

    val clamped = score.coerceIn(0, PASSWORD_STRENGTH_MAX_SCORE)
    val level = when {
        clamped <= 1 -> PasswordStrengthLevel.VERY_WEAK
        clamped == 2 -> PasswordStrengthLevel.WEAK
        clamped == 3 -> PasswordStrengthLevel.FAIR
        clamped == 4 -> PasswordStrengthLevel.GOOD
        else -> PasswordStrengthLevel.STRONG
    }
    return PasswordStrengthResult(score = clamped, level = level)
}

private fun containsCommonWord(password: String): Boolean {
    val dictionary = listOf(
        "password", "pass", "passwd", "admin", "administrator",
        "root", "welcome", "letmein", "login", "default",
        "secret", "iloveyou", "qwerty", "abc123", "123456",
        "12345678", "111111", "000000", "666666", "888888",
        "dragon", "monkey", "sunshine", "football", "baseball",
        "trustno1", "princess", "master", "backup", "archive"
    )
    return dictionary.any { password.contains(it) }
}

private fun hasLongSequentialRun(password: String): Boolean {
    if (password.length < 4) return false
    var ascRun = 1
    var descRun = 1
    for (i in 1 until password.length) {
        val diff = password[i].code - password[i - 1].code
        ascRun = if (diff == 1) ascRun + 1 else 1
        descRun = if (diff == -1) descRun + 1 else 1
        if (ascRun >= 4 || descRun >= 4) return true
    }
    return false
}

private fun hasKeyboardPattern(password: String): Boolean {
    if (password.length < 4) return false
    val keyboardRows = listOf(
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm",
        "1234567890"
    )
    return keyboardRows.any { row ->
        containsLinearSubsequence(password, row, 4) ||
                containsLinearSubsequence(password, row.reversed(), 4)
    }
}

private fun containsLinearSubsequence(input: String, sequence: String, minLen: Int): Boolean {
    if (input.length < minLen) return false
    val maxLen = minOf(6, input.length)
    for (len in minLen..maxLen) {
        for (start in 0..input.length - len) {
            val part = input.substring(start, start + len)
            if (sequence.contains(part)) return true
        }
    }
    return false
}

private fun hasRepeatingChunk(password: String): Boolean {
    if (password.length < 6) return false
    for (size in 2..(password.length / 2)) {
        for (start in 0..(password.length - size * 2)) {
            val chunk = password.substring(start, start + size)
            val next = password.substring(start + size, start + size * 2)
            if (chunk == next) return true
        }
    }
    return false
}

private fun looksLikeDateOrYear(password: String): Boolean {
    if (password.matches(Regex("^(19|20)\\d{2}$"))) return true
    if (password.matches(Regex("^(19|20)\\d{6}$"))) return true // yyyymmdd
    if (password.matches(Regex("^\\d{8}$"))) return true // e.g. mmddyyyy/ddmmyyyy
    if (password.matches(Regex("^\\d{4}[-_/]\\d{2}[-_/]\\d{2}$"))) return true
    return false
}

fun generateStrongPassword(length: Int): String {
    val safeLength = length.coerceAtLeast(MIN_GENERATED_PASSWORD_LENGTH)
    val lower = "abcdefghijklmnopqrstuvwxyz"
    val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val digits = "0123456789"
    val symbols = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"
    val all = lower + upper + digits + symbols
    val chars = mutableListOf(
        lower.randomChar(),
        upper.randomChar(),
        digits.randomChar(),
        symbols.randomChar()
    )
    repeat(safeLength - chars.size) {
        chars += all.randomChar()
    }
    for (i in chars.lastIndex downTo 1) {
        val j = secureRandomIntPlatform(i + 1)
        val temp = chars[i]
        chars[i] = chars[j]
        chars[j] = temp
    }
    return chars.joinToString("")
}

private fun String.randomChar(): Char = this[secureRandomIntPlatform(this.length)]
