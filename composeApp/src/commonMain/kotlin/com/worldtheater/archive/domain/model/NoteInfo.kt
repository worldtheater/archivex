package com.worldtheater.archive.domain.model

import androidx.compose.runtime.Immutable
import com.worldtheater.archive.platform.system.currentTimeMillis

@Immutable
data class NoteInfo(
    val _id: Int = 0,
    val nodeId: String = "",
    val title: String = "",
    val body: String,
    val createTime: Long,
    val updateTime: Long,
    val color: Int,
    val itemType: Int = ITEM_TYPE_NOTE,
    val parentNodeId: String? = null,
    val isSensitive: Boolean = false,
    // True when body is a list-preview snippet (not full persisted content).
    val isBodyPreview: Boolean = false
) {
    override fun toString(): String {
        return "NoteInfo(_id=$_id, nodeId='$nodeId', title='$title', body=<redacted,len=${body.length}>, createTime=$createTime, updateTime=$updateTime, color=$color, itemType=$itemType, parentNodeId=$parentNodeId, isSensitive=$isSensitive, isBodyPreview=$isBodyPreview)"
    }
}

const val ITEM_TYPE_NOTE = 0
const val ITEM_TYPE_FOLDER = 1

fun emptyNoteInfo(): NoteInfo = NoteInfo(
    _id = 0,
    title = "",
    body = "",
    createTime = currentTimeMillis(),
    updateTime = currentTimeMillis(),
    color = 0,
    itemType = ITEM_TYPE_NOTE,
    parentNodeId = null,
    isSensitive = false
)
