package com.worldtheater.archive.domain.model

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

val NoteInfoSaver: Saver<NoteInfo, Any> = listSaver(
    save = { note ->
        listOf(
            note._id,
            note.nodeId,
            note.title,
            note.body,
            note.createTime,
            note.updateTime,
            note.color,
            note.itemType,
            note.parentNodeId,
            note.isSensitive,
            note.isBodyPreview
        )
    },
    restore = { saved ->
        NoteInfo(
            _id = (saved[0] as Number).toInt(),
            nodeId = saved[1] as String,
            title = saved[2] as String,
            body = saved[3] as String,
            createTime = (saved[4] as Number).toLong(),
            updateTime = (saved[5] as Number).toLong(),
            color = (saved[6] as Number).toInt(),
            itemType = (saved[7] as Number).toInt(),
            parentNodeId = saved[8] as String?,
            isSensitive = saved[9] as Boolean,
            isBodyPreview = saved[10] as Boolean
        )
    }
)
