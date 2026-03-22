package com.worldtheater.archive.domain.repository.impl

import com.worldtheater.archive.data.local.Note
import com.worldtheater.archive.domain.model.NoteInfo

object Converter {

    fun note2NoteInfo(inData: Note): NoteInfo = NoteInfo(
        _id = inData._id,
        nodeId = inData.nodeId,
        title = inData.title,
        body = inData.body,
        createTime = inData.createTime,
        updateTime = inData.updateTime,
        color = inData.color,
        itemType = inData.itemType,
        parentNodeId = inData.parentNodeId,
        isSensitive = inData.isSensitive,
        isBodyPreview = false
    )

    fun noteInfo2Note(inData: NoteInfo): Note = Note(
        _id = inData._id,
        nodeId = inData.nodeId,
        title = inData.title,
        body = inData.body,
        createTime = inData.createTime,
        updateTime = inData.updateTime,
        color = inData.color,
        itemType = inData.itemType,
        parentNodeId = inData.parentNodeId,
        isSensitive = inData.isSensitive
    )
}