package com.worldtheater.archive.platform.data

import com.worldtheater.archive.data.local.Note
import com.worldtheater.archive.platform.system.generateUuidString

internal data class LegacyNoteRow(
    val body: String,
    val legacyDateText: String,
    val color: Int
)

internal fun restoreNotesFromLegacyRows(
    rows: List<LegacyNoteRow>,
    parseLegacyDate: (String) -> Long,
    onProgress: (completedSteps: Int, totalSteps: Int) -> Unit
): List<Note> {
    val uiTotal = rows.size.coerceAtLeast(1)
    return buildList(rows.size) {
        rows.forEachIndexed { index, row ->
            val legacyDate = parseLegacyDate(row.legacyDateText)
            add(
                Note(
                    nodeId = generateUuidString(),
                    title = "",
                    body = row.body,
                    createTime = legacyDate,
                    updateTime = legacyDate,
                    color = row.color
                )
            )
            onProgress(((index + 1) / 2).coerceAtMost(uiTotal), uiTotal)
        }
    }
}
