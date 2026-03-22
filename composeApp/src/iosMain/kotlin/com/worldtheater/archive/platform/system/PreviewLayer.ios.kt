@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.worldtheater.archive.platform.system

import kotlinx.cinterop.useContents
import platform.UIKit.UIScreen

actual fun shouldDisposePreviewLayerWhileEditing(): Boolean = false

actual fun shouldEnablePreviewTextSelection(): Boolean = false

actual fun shouldUseLazyPreviewScroll(): Boolean = false

actual fun shouldSyncPreviewAndEditScroll(): Boolean = true

actual fun previewMermaidMaxDisplayHeightDp(): Int =
    (UIScreen.mainScreen.bounds.useContents { size.height } / 2.0).toInt().coerceAtLeast(180)

actual fun logPreviewNoteForDebug(title: String, body: String) {
    println("[Archive][PreviewNote][BEGIN]")
    println("title=$title")
    println("body<<EOF")
    println(body)
    println("EOF")
    println("[Archive][PreviewNote][END]")
}
