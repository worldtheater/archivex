package com.worldtheater.archive.platform.system

import androidx.compose.runtime.Composable
import java.util.UUID

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun elapsedRealtimeMillis(): Long = System.nanoTime() / 1_000_000L

actual fun generateUuidString(): String = UUID.randomUUID().toString()

actual fun shouldDisposePreviewLayerWhileEditing(): Boolean = false

actual fun shouldEnablePreviewTextSelection(): Boolean = false

actual fun shouldUseLazyPreviewScroll(): Boolean = false

actual fun shouldSyncPreviewAndEditScroll(): Boolean = true

actual fun previewMermaidMaxDisplayHeightDp(): Int = 420

actual fun logPreviewNoteForDebug(title: String, body: String) = Unit

@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
