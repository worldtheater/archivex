package com.worldtheater.archive.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownUtilsTest {

    @Test
    fun toggleBold_wrapsSelectionAndKeepsInnerSelection() {
        val value = TextFieldValue("hello", selection = TextRange(0, 5))
        val result = MarkdownUtils.toggleBold(value)
        assertEquals("**hello**", result.text)
        assertEquals(2, result.selection.start)
        assertEquals(7, result.selection.end)
    }

    @Test
    fun toggleBold_insertsMarkersAtCursor() {
        val value = TextFieldValue("abc", selection = TextRange(1))
        val result = MarkdownUtils.toggleBold(value)
        assertEquals("a****bc", result.text)
        assertEquals(3, result.selection.start)
        assertEquals(3, result.selection.end)
    }

    @Test
    fun toggleBold_removesInsertedMarkersWhenCursorHasNotMoved() {
        val inserted = MarkdownUtils.toggleBold(TextFieldValue("abc", selection = TextRange(1)))
        val removed = MarkdownUtils.toggleBold(inserted)
        assertEquals("abc", removed.text)
        assertEquals(1, removed.selection.start)
        assertEquals(1, removed.selection.end)
    }

    @Test
    fun toggleItalic_removesInsertedMarkersWhenCursorHasNotMoved() {
        val inserted = MarkdownUtils.toggleItalic(TextFieldValue("abc", selection = TextRange(1)))
        val removed = MarkdownUtils.toggleItalic(inserted)
        assertEquals("abc", removed.text)
        assertEquals(1, removed.selection.start)
        assertEquals(1, removed.selection.end)
    }

    @Test
    fun toggleCode_removesInsertedMarkersWhenCursorHasNotMoved() {
        val inserted = MarkdownUtils.toggleCode(TextFieldValue("abc", selection = TextRange(1)))
        val removed = MarkdownUtils.toggleCode(inserted)
        assertEquals("abc", removed.text)
        assertEquals(1, removed.selection.start)
        assertEquals(1, removed.selection.end)
    }

    @Test
    fun toggleBold_unwrapsSelectionWhenAlreadyWrapped() {
        val wrapped = MarkdownUtils.toggleBold(TextFieldValue("hello", selection = TextRange(0, 5)))
        val unwrapped = MarkdownUtils.toggleBold(wrapped)
        assertEquals("hello", unwrapped.text)
        assertEquals(0, unwrapped.selection.start)
        assertEquals(5, unwrapped.selection.end)
    }

    @Test
    fun toggleItalic_unwrapsSelectionWhenAlreadyWrapped() {
        val wrapped = MarkdownUtils.toggleItalic(TextFieldValue("hello", selection = TextRange(1, 4)))
        val unwrapped = MarkdownUtils.toggleItalic(wrapped)
        assertEquals("hello", unwrapped.text)
        assertEquals(1, unwrapped.selection.start)
        assertEquals(4, unwrapped.selection.end)
    }

    @Test
    fun toggleCode_unwrapsSelectionWhenAlreadyWrapped() {
        val wrapped = MarkdownUtils.toggleCode(TextFieldValue("hello", selection = TextRange(1, 4)))
        val unwrapped = MarkdownUtils.toggleCode(wrapped)
        assertEquals("hello", unwrapped.text)
        assertEquals(1, unwrapped.selection.start)
        assertEquals(4, unwrapped.selection.end)
    }
}
