package com.worldtheater.archive.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

object MarkdownUtils {
    fun toggleBold(value: TextFieldValue): TextFieldValue {
        return toggleWrapper(value, "**")
    }

    fun toggleItalic(value: TextFieldValue): TextFieldValue {
        return toggleWrapper(value, "*")
    }

    fun toggleCode(value: TextFieldValue): TextFieldValue {
        return toggleWrapper(value, "`")
    }

    private fun toggleWrapper(value: TextFieldValue, wrapper: String): TextFieldValue {
        val text = value.text
        val selection = value.selection

        return if (selection.collapsed) {
            val cursor = selection.start
            val hasWrapperBefore = cursor >= wrapper.length &&
                    text.substring(cursor - wrapper.length, cursor) == wrapper
            val hasWrapperAfter = cursor + wrapper.length <= text.length &&
                    text.substring(cursor, cursor + wrapper.length) == wrapper

            if (hasWrapperBefore && hasWrapperAfter) {
                val newText = text.removeRange(cursor - wrapper.length, cursor + wrapper.length)
                val newCursor = cursor - wrapper.length
                value.copy(text = newText, selection = TextRange(newCursor))
            } else {
                val newText = text.replaceRange(selection.start, selection.end, "$wrapper$wrapper")
                val newCursor = selection.start + wrapper.length
                value.copy(text = newText, selection = TextRange(newCursor))
            }
        } else {
            val hasWrapperBefore = selection.start >= wrapper.length &&
                    text.substring(selection.start - wrapper.length, selection.start) == wrapper
            val hasWrapperAfter = selection.end + wrapper.length <= text.length &&
                    text.substring(selection.end, selection.end + wrapper.length) == wrapper

            if (hasWrapperBefore && hasWrapperAfter) {
                val newText = buildString(text.length - wrapper.length * 2) {
                    append(text, 0, selection.start - wrapper.length)
                    append(text, selection.start, selection.end)
                    append(text, selection.end + wrapper.length, text.length)
                }
                val newStart = selection.start - wrapper.length
                val newEnd = selection.end - wrapper.length
                value.copy(text = newText, selection = TextRange(newStart, newEnd))
            } else {
                val selectedText = text.substring(selection.start, selection.end)
                val newText = text.replaceRange(
                    selection.start,
                    selection.end,
                    "$wrapper$selectedText$wrapper"
                )
                val newStart = selection.start + wrapper.length
                val newEnd = selection.end + wrapper.length
                value.copy(text = newText, selection = TextRange(newStart, newEnd))
            }
        }
    }

    fun insertHeader(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val selection = value.selection
        val lineStart = text.lastIndexOf('\n', selection.start - 1) + 1
        val matchResult = Regex("^(#+) ").find(text.substring(lineStart))

        if (matchResult != null) {
            val fullMatch = matchResult.value
            val hashes = matchResult.groupValues[1]

            if (hashes.length < 6) {
                val newHashes = hashes + "#"
                val newPrefix = "$newHashes "
                val newText = text.replaceRange(lineStart, lineStart + fullMatch.length, newPrefix)
                val newCursor = selection.start + 1
                return value.copy(text = newText, selection = TextRange(newCursor))
            } else {
                val newText = text.removeRange(lineStart, lineStart + fullMatch.length)
                val newCursor = selection.start - fullMatch.length
                return value.copy(
                    text = newText,
                    selection = TextRange(if (newCursor < lineStart) lineStart else newCursor)
                )
            }
        } else {
            return insertLinePrefix(value, "# ")
        }
    }

    fun insertList(value: TextFieldValue): TextFieldValue {
        return insertLinePrefix(value, "- ")
    }

    fun insertQuote(value: TextFieldValue): TextFieldValue {
        return insertLinePrefix(value, "> ")
    }

    private fun insertLinePrefix(value: TextFieldValue, prefix: String): TextFieldValue {
        val text = value.text
        val selection = value.selection
        val lineStart = text.lastIndexOf('\n', selection.start - 1) + 1

        if (text.startsWith(prefix, lineStart)) {
            val newText = text.removeRange(lineStart, lineStart + prefix.length)
            var newCursor = selection.start - prefix.length
            if (newCursor < lineStart) newCursor = lineStart
            return value.copy(text = newText, selection = TextRange(newCursor))
        } else {
            val newText = text.replaceRange(lineStart, lineStart, prefix)
            val newCursor = selection.start + prefix.length
            return value.copy(text = newText, selection = TextRange(newCursor))
        }
    }
}
