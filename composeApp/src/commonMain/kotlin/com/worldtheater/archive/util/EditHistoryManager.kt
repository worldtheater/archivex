package com.worldtheater.archive.util

import androidx.compose.ui.text.input.TextFieldValue

class EditHistoryManager(
    private val maxSteps: Int = 30
) {
    private val undoStack: ArrayDeque<TextFieldValue> = ArrayDeque()
    private val redoStack: ArrayDeque<TextFieldValue> = ArrayDeque()

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun recordBeforeChange(current: TextFieldValue) {
        if (undoStack.isNotEmpty() &&
            undoStack.last().text == current.text &&
            undoStack.last().selection == current.selection
        ) {
            return
        }
        undoStack.addLast(current)
        while (undoStack.size > maxSteps) {
            undoStack.removeFirst()
        }
        redoStack.clear()
    }

    fun undo(current: TextFieldValue): TextFieldValue? {
        if (undoStack.isEmpty()) return null
        redoStack.addLast(current)
        return undoStack.removeLast()
    }

    fun redo(current: TextFieldValue): TextFieldValue? {
        if (redoStack.isEmpty()) return null
        undoStack.addLast(current)
        while (undoStack.size > maxSteps) {
            undoStack.removeFirst()
        }
        return redoStack.removeLast()
    }

    fun reset() {
        undoStack.clear()
        redoStack.clear()
    }
}
