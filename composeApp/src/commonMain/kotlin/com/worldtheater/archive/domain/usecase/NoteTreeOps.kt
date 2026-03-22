package com.worldtheater.archive.domain.usecase

import com.worldtheater.archive.domain.model.ITEM_TYPE_FOLDER
import com.worldtheater.archive.domain.model.NoteInfo

object NoteTreeOps {

    fun countDescendants(noteList: List<NoteInfo>, rootFolderId: Int): Int {
        val rootNodeId = noteList.find { it._id == rootFolderId }?.nodeId ?: return 0
        val childrenByParent = noteList.groupBy { it.parentNodeId }
        val visitedItemIds = mutableSetOf<Int>()
        val visitedFolderNodeIds = mutableSetOf(rootNodeId)
        val stack = ArrayDeque<String>()
        stack.add(rootNodeId)
        var count = 0

        while (stack.isNotEmpty()) {
            val parentNodeId = stack.removeLast()
            childrenByParent[parentNodeId].orEmpty().forEach { child ->
                if (!visitedItemIds.add(child._id)) return@forEach
                count += 1
                if (child.itemType == ITEM_TYPE_FOLDER && visitedFolderNodeIds.add(child.nodeId)) {
                    stack.add(child.nodeId)
                }
            }
        }
        return count
    }

    fun buildItemPath(noteList: List<NoteInfo>, item: NoteInfo): String {
        val noteByNodeId = noteList.associateBy { it.nodeId }
        val visited = mutableSetOf<String>()
        val names = mutableListOf<String>()
        var cursor: NoteInfo? = item
        while (cursor != null) {
            val key = cursor.nodeId.takeIf { it.isNotBlank() } ?: "id:${cursor._id}"
            if (!visited.add(key)) break
            names += cursor.title.ifBlank { "#${cursor._id}" }
            cursor = cursor.parentNodeId?.let { parentNodeId ->
                noteByNodeId[parentNodeId]
            }
        }
        return "/" + names.asReversed().joinToString("/")
    }

    fun buildNoteById(noteList: List<NoteInfo>): Map<Int, NoteInfo> {
        return noteList.associateBy { it._id }
    }

    fun buildParentIdByNodeId(noteList: List<NoteInfo>): Map<String, Int> {
        return noteList.associate { it.nodeId to it._id }
    }

    fun buildSortedChildrenByParent(noteList: List<NoteInfo>): Map<String?, List<NoteInfo>> {
        return noteList
            .groupBy { it.parentNodeId }
            .mapValues { (_, items) ->
                items.sortedWith { a, b ->
                    val aFolder = a.itemType == ITEM_TYPE_FOLDER
                    val bFolder = b.itemType == ITEM_TYPE_FOLDER
                    when {
                        aFolder && !bFolder -> -1
                        !aFolder && bFolder -> 1
                        aFolder && bFolder -> a.title.lowercase().compareTo(b.title.lowercase())
                        else -> b._id.compareTo(a._id)
                    }
                }
            }
    }

    fun visibleItemsForFolder(
        noteList: List<NoteInfo>,
        noteById: Map<Int, NoteInfo>,
        sortedChildrenByParent: Map<String?, List<NoteInfo>>,
        folderId: Int?,
        searchQuery: String
    ): List<NoteInfo> {
        if (searchQuery.isNotBlank()) {
            return noteList
        }
        val parentNodeId = folderId?.let { id -> noteById[id]?.nodeId }
        return sortedChildrenByParent[parentNodeId].orEmpty()
    }

    fun isDescendant(
        descendantNodeId: String,
        ancestorNodeId: String,
        notes: List<NoteInfo>
    ): Boolean {
        var cursor: String? = descendantNodeId
        val byNodeId = notes.associateBy { it.nodeId }
        while (cursor != null) {
            if (cursor == ancestorNodeId) return true
            cursor = byNodeId[cursor]?.parentNodeId
        }
        return false
    }

    fun computeDepth(parentNodeId: String?, notes: List<NoteInfo>): Int {
        var depth = 0
        var cursor = parentNodeId
        val byNodeId = notes.associateBy { it.nodeId }
        while (cursor != null) {
            depth += 1
            cursor = byNodeId[cursor]?.parentNodeId
        }
        return depth
    }

    fun computeSubtreeHeight(rootFolderId: Int, notes: List<NoteInfo>): Int {
        val rootNodeId = notes.find { it._id == rootFolderId }?.nodeId ?: return 1
        val children =
            notes.filter { it.parentNodeId == rootNodeId && it.itemType == ITEM_TYPE_FOLDER }
        if (children.isEmpty()) return 1
        return 1 + (children.maxOfOrNull { computeSubtreeHeight(it._id, notes) } ?: 0)
    }

    fun parentNodeIdById(id: Int, notes: List<NoteInfo>): String? {
        return notes.find { it._id == id }?.nodeId
    }
}
