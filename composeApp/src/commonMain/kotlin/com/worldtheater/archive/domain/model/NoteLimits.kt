package com.worldtheater.archive.domain.model

const val MAX_TOTAL_ITEMS = 20000
const val MAX_NOTE_BODY_LENGTH = 50000
const val MAX_FOLDER_DIRECT_ITEMS = 600
const val MAX_FOLDER_DEPTH = 6
const val MAX_FOLDER_NAME_LENGTH = 30

enum class NoteLimitType {
    TOTAL_ITEMS,
    NOTE_BODY_LENGTH,
    FOLDER_DIRECT_ITEMS
}

class NoteLimitException(
    val type: NoteLimitType,
    val limit: Int
) : IllegalStateException("Note limit exceeded: $type ($limit)")
