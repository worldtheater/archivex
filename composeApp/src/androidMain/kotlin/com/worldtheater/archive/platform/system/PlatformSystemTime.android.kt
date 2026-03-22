package com.worldtheater.archive.platform.system

import android.content.res.Resources
import android.os.SystemClock
import java.util.UUID

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()

actual fun generateUuidString(): String = UUID.randomUUID().toString()

actual fun shouldDisposePreviewLayerWhileEditing(): Boolean = false

actual fun shouldEnablePreviewTextSelection(): Boolean = false

actual fun shouldUseLazyPreviewScroll(): Boolean = false

actual fun shouldSyncPreviewAndEditScroll(): Boolean = true

actual fun previewMermaidMaxDisplayHeightDp(): Int =
    ((Resources.getSystem().displayMetrics.heightPixels / Resources.getSystem().displayMetrics.density) / 2f)
        .toInt()
        .coerceAtLeast(180)

actual fun logPreviewNoteForDebug(title: String, body: String) = Unit
