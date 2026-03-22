package com.worldtheater.archive.util

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object StringUtils {

    private const val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private val hexChars = "0123456789ABCDEF".toCharArray()

    @OptIn(ExperimentalEncodingApi::class)
    fun ByteArray.b64Encode() = Base64.encode(this)

    @OptIn(ExperimentalEncodingApi::class)
    fun String.b64Decode() = Base64.decode(this)

    fun String.b64DecodeToHex() = b64Decode().toHexString()

    fun ByteArray.toHexString(): String {
        val hex = CharArray(2 * this.size)
        this.forEachIndexed { i, byte ->
            val unsigned = 0xff and byte.toInt()
            hex[2 * i] = hexChars[unsigned / 16]
            hex[2 * i + 1] = hexChars[unsigned % 16]
        }
        return hex.joinToString("")
    }

    fun getRandomString(length: Int): String {
        return (1..length).map { charset.random() }.joinToString("")
    }

    fun smartSplit(text: String): List<String> {
        val result = mutableListOf<String>()
        val buffer = StringBuilder()

        for (i in text.indices) {
            val char = text[i]

            if (char == '\n') {
                if (buffer.isNotEmpty()) {
                    result.add(buffer.toString())
                    buffer.clear()
                }
            } else if (char == ' ') {
                val prev = text.getOrNull(i - 1)
                val next = text.getOrNull(i + 1)

                val isPrevLatin = prev != null && !isCJK(prev) && !prev.isWhitespace()
                val isNextLatin = next != null && !isCJK(next) && !next.isWhitespace()

                if (isPrevLatin && isNextLatin) {
                    buffer.append(char)
                } else {
                    if (buffer.isNotEmpty()) {
                        result.add(buffer.toString())
                        buffer.clear()
                    }
                }
            } else {
                buffer.append(char)
            }
        }
        if (buffer.isNotEmpty()) {
            result.add(buffer.toString())
        }
        return result
    }

    private fun isCJK(c: Char): Boolean {
        val code = c.code
        return code in 0x4E00..0x9FFF || // CJK Unified Ideographs
                code in 0x3400..0x4DBF || // CJK Extension A
                code in 0xF900..0xFAFF || // CJK Compatibility Ideographs
                code in 0x3000..0x303F || // CJK Symbols and Punctuation
                code in 0x2000..0x206F || // General Punctuation
                code in 0xFF00..0xFFEF // Halfwidth and Fullwidth Forms
    }
}
