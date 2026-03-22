package com.worldtheater.archive.feature.debug

import com.worldtheater.archive.domain.repository.NoteRepository
import com.worldtheater.archive.domain.model.ITEM_TYPE_FOLDER
import com.worldtheater.archive.domain.model.NoteInfo
import com.worldtheater.archive.domain.model.NoteLimitException
import com.worldtheater.archive.platform.system.currentTimeMillis
import com.worldtheater.archive.platform.system.generateUuidString

data class DebugGenerateResult(
    val createdCount: Int,
    val limitException: NoteLimitException? = null
)

enum class DebugContentStyle {
    PLAIN_TEXT,
    CHIP_PHRASES
}

class DebugDataGenerator(
    private val noteRepository: NoteRepository
) {
    suspend fun generateBenchmarkDataset(
        totalCount: Int,
        onProgress: (createdCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): DebugGenerateResult {
        // Benchmark-friendly dataset:
        // - total items configurable by totalCount
        // - includes folders + plain text notes + chip-style notes
        // - mix of titled and untitled notes
        val totalTarget = totalCount.coerceAtLeast(1)
        val rootFolderCount = (totalTarget / 60).coerceIn(5, 60)
        val remainingAfterFolders = (totalTarget - rootFolderCount).coerceAtLeast(0)
        val childNotesTarget = (remainingAfterFolders * 4) / 5
        val childNotesPerFolder = if (rootFolderCount > 0) {
            childNotesTarget / rootFolderCount
        } else {
            0
        }
        val rootNotesCount = remainingAfterFolders - (childNotesPerFolder * rootFolderCount)

        var created = 0
        onProgress(created, totalTarget)

        val folderNodeIds = mutableListOf<String>()
        val batch = ArrayList<NoteInfo>(totalTarget)
        repeat(rootFolderCount) { folderIndex ->
            val now = currentTimeMillis()
            val nodeId = generateUuidString()
            val folder = NoteInfo(
                nodeId = nodeId,
                title = "BM-FOLDER-${folderIndex + 1}",
                body = "",
                createTime = now,
                updateTime = now,
                color = 0,
                itemType = ITEM_TYPE_FOLDER
            )
            batch += folder
            folderNodeIds += nodeId
        }

        folderNodeIds.forEachIndexed { folderIndex, parentNodeId ->
            repeat(childNotesPerFolder) { childIndex ->
                val now = currentTimeMillis()
                val useChip = ((folderIndex + childIndex) % 2 == 0)
                val includeTitle = ((folderIndex + childIndex) % 3 != 0)
                val note = NoteInfo(
                    title = if (includeTitle) "BM-C-${folderIndex + 1}-${childIndex + 1}" else "",
                    body = buildDebugBody(
                        length = if (useChip) 120 else 320,
                        style = if (useChip) DebugContentStyle.CHIP_PHRASES else DebugContentStyle.PLAIN_TEXT
                    ),
                    createTime = now,
                    updateTime = now,
                    color = 0,
                    parentNodeId = parentNodeId
                )
                batch += note
            }
        }

        repeat(rootNotesCount) { index ->
            val now = currentTimeMillis()
            val useChip = index % 2 == 1
            val includeTitle = index % 4 != 0
            val note = NoteInfo(
                title = if (includeTitle) "BM-R-${index + 1}" else "",
                body = buildDebugBody(
                    length = if (useChip) 100 else 360,
                    style = if (useChip) DebugContentStyle.CHIP_PHRASES else DebugContentStyle.PLAIN_TEXT
                ),
                createTime = now,
                updateTime = now,
                color = 0
            )
            batch += note
        }

        try {
            noteRepository.addNotesBatch(batch) { inserted, total ->
                created = inserted
                if (shouldEmitProgress(inserted, total)) {
                    onProgress(inserted, total)
                }
            }
        } catch (e: NoteLimitException) {
            return DebugGenerateResult(createdCount = created, limitException = e)
        }

        return DebugGenerateResult(createdCount = created)
    }

    suspend fun generateRootNotes(
        count: Int,
        bodyLength: Int,
        contentStyle: DebugContentStyle,
        includeTitle: Boolean,
        onProgress: (createdCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): DebugGenerateResult {
        if (count <= 0 || bodyLength <= 0) return DebugGenerateResult(createdCount = 0)
        val body = buildDebugBody(bodyLength, contentStyle)
        var created = 0
        onProgress(created, count)
        val batch = ArrayList<NoteInfo>(count)
        repeat(count) { index ->
            val now = currentTimeMillis()
            val note = NoteInfo(
                title = if (includeTitle) "DEBUG-${now}-${index + 1}" else "",
                body = body,
                createTime = now,
                updateTime = now,
                color = 0
            )
            batch += note
        }
        try {
            noteRepository.addNotesBatch(batch) { inserted, total ->
                created = inserted
                if (shouldEmitProgress(inserted, total)) {
                    onProgress(inserted, total)
                }
            }
        } catch (e: NoteLimitException) {
            return DebugGenerateResult(createdCount = created, limitException = e)
        }
        return DebugGenerateResult(createdCount = created)
    }

    suspend fun generateFolderWithChildren(
        childCount: Int,
        bodyLength: Int,
        contentStyle: DebugContentStyle,
        includeTitle: Boolean,
        onProgress: (createdCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): DebugGenerateResult {
        if (childCount <= 0 || bodyLength <= 0) return DebugGenerateResult(createdCount = 0)
        val now = currentTimeMillis()
        val folderNodeId = generateUuidString()
        val folder = NoteInfo(
            nodeId = folderNodeId,
            title = "DEBUG-FOLDER-${now}",
            body = "",
            createTime = now,
            updateTime = now,
            color = 0,
            itemType = ITEM_TYPE_FOLDER
        )
        val batch = ArrayList<NoteInfo>(childCount + 1)
        batch += folder

        val body = buildDebugBody(bodyLength, contentStyle)
        repeat(childCount) { index ->
            val ts = currentTimeMillis()
            batch += NoteInfo(
                title = if (includeTitle) "DEBUG-CHILD-${index + 1}" else "",
                body = body,
                createTime = ts,
                updateTime = ts,
                color = 0,
                parentNodeId = folderNodeId
            )
        }

        var inserted = 0
        val total = batch.size
        onProgress(0, total)
        try {
            noteRepository.addNotesBatch(batch) { completed, all ->
                inserted = completed
                if (shouldEmitProgress(completed, all)) {
                    onProgress(completed, all)
                }
            }
        } catch (e: NoteLimitException) {
            return DebugGenerateResult(createdCount = inserted, limitException = e)
        }
        return DebugGenerateResult(createdCount = inserted)
    }

    private fun shouldEmitProgress(inserted: Int, total: Int): Boolean {
        return inserted == 0 || inserted == total || inserted % 50 == 0
    }

    private fun buildDebugBody(length: Int, style: DebugContentStyle): String {
        val safeLen = length.coerceAtLeast(1)
        val seed = when (style) {
            DebugContentStyle.PLAIN_TEXT -> "debug-payload-"
            DebugContentStyle.CHIP_PHRASES -> "alpha  beta  gamma  delta  epsilon  zeta  eta  theta  "
        }
        return buildString(safeLen) {
            while (this.length < safeLen) {
                append(seed)
            }
            if (this.length > safeLen) {
                setLength(safeLen)
            }
        }
    }
}
