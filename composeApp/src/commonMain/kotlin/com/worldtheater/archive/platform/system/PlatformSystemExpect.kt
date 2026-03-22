package com.worldtheater.archive.platform.system

expect fun currentTimeMillis(): Long

expect fun elapsedRealtimeMillis(): Long

expect fun generateUuidString(): String

expect fun shouldDisposePreviewLayerWhileEditing(): Boolean

expect fun shouldEnablePreviewTextSelection(): Boolean

expect fun shouldUseLazyPreviewScroll(): Boolean

expect fun shouldSyncPreviewAndEditScroll(): Boolean

expect fun previewMermaidMaxDisplayHeightDp(): Int

expect fun logPreviewNoteForDebug(title: String, body: String)

expect fun defaultPlatformSystemProvider(): PlatformSystemProvider
