@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.worldtheater.archive.ui.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import archivex.composeapp.generated.resources.Res
import archivex.composeapp.generated.resources.action_cancel
import archivex.composeapp.generated.resources.menu_export_mermaid_svg
import archivex.composeapp.generated.resources.msg_mermaid_svg_export_failure_fmt
import archivex.composeapp.generated.resources.msg_mermaid_svg_export_success_fmt
import com.worldtheater.archive.platform.system.AppBackHandler
import com.worldtheater.archive.platform.system.DefaultUserMessageSink
import com.worldtheater.archive.ui.theme.DefaultAppBarButtonSize
import com.worldtheater.archive.ui.theme.isAppInDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.getString
import androidx.compose.ui.input.pointer.PointerInputChange
import java.io.File
import java.nio.charset.StandardCharsets
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

private const val JVM_IMAGE_PREVIEW_MIN_SCALE = 1f
private const val JVM_IMAGE_PREVIEW_MAX_SCALE = 2f
private const val JVM_IMAGE_PREVIEW_SCROLL_ZOOM_STEP = 1.14f

@Composable
actual fun ImagePreviewScreen(
    image: ImagePreviewData,
    onDismiss: () -> Unit
) {
    val isDarkTheme = isAppInDarkTheme()
    val scope = rememberCoroutineScope()
    val userMessageSink = remember { DefaultUserMessageSink() }
    val backgroundColor = if (isDarkTheme) {
        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.96f)
    } else {
        MaterialTheme.colorScheme.background
    }
    val contentColor = if (isDarkTheme) {
        androidx.compose.ui.graphics.Color.White
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    var scale by remember(image) { mutableFloatStateOf(1f) }
    var offset by remember(image) { mutableStateOf(Offset.Zero) }
    var contextMenuExpanded by remember(image) { mutableStateOf(false) }
    var contextMenuOffset by remember(image) { mutableStateOf(Offset.Zero) }

    AppBackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            val viewportSize = with(density) {
                Size(
                    width = (maxWidth - 48.dp).toPx().coerceAtLeast(0f),
                    height = (maxHeight - 48.dp).toPx().coerceAtLeast(0f)
                )
            }
            val imageAspectRatio = remember(image.widthDp, image.heightDp) {
                if (image.widthDp <= 0 || image.heightDp <= 0) {
                    1f
                } else {
                    image.widthDp.toFloat() / image.heightDp.toFloat()
                }
            }
            val fittedImageSize = remember(viewportSize, imageAspectRatio) {
                fitImageIntoViewport(
                    viewportSize = viewportSize,
                    imageAspectRatio = imageAspectRatio
                )
            }

            fun boundedOffset(rawOffset: Offset, scaleValue: Float): Offset {
                val maxOffset = calculateMaxOffset(
                    viewportSize = viewportSize,
                    contentSize = fittedImageSize,
                    scale = scaleValue
                )
                return rawOffset.clamp(maxOffset)
            }

            val imageFrameModifier = Modifier
                .width(with(density) { fittedImageSize.width.toDp() })
                .height(with(density) { fittedImageSize.height.toDp() })
                .padding(24.dp)

            Box(modifier = imageFrameModifier) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onPointerEvent(PointerEventType.Press) { event ->
                            if (image.svgExport == null || !event.buttons.isSecondaryPressed) return@onPointerEvent
                            val clickPosition = event.changes.firstOrNull()?.position ?: return@onPointerEvent
                            contextMenuOffset = clickPosition
                            contextMenuExpanded = true
                            event.changes.forEach { it.consume() }
                        }
                        .onPointerEvent(PointerEventType.Scroll) { event ->
                            val deltaY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                            if (deltaY == 0f) return@onPointerEvent
                            val factor = if (deltaY > 0f) {
                                1f / JVM_IMAGE_PREVIEW_SCROLL_ZOOM_STEP
                            } else {
                                JVM_IMAGE_PREVIEW_SCROLL_ZOOM_STEP
                            }
                            scale = (scale * factor).coerceIn(
                                JVM_IMAGE_PREVIEW_MIN_SCALE,
                                JVM_IMAGE_PREVIEW_MAX_SCALE
                            )
                            offset = boundedOffset(offset, scale)
                            event.changes.forEach { it.consume() }
                        }
                        .pointerInput(scale, viewportSize, fittedImageSize) {
                            detectDragGestures(
                                onDrag = { change: PointerInputChange, dragAmount: Offset ->
                                    if (scale <= JVM_IMAGE_PREVIEW_MIN_SCALE) return@detectDragGestures
                                    change.consume()
                                    offset = boundedOffset(
                                        rawOffset = offset + Offset(dragAmount.x, dragAmount.y),
                                        scaleValue = scale
                                    )
                                }
                            )
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                ) {
                    Image(
                        bitmap = image.bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                DropdownMenu(
                    expanded = contextMenuExpanded && image.svgExport != null,
                    onDismissRequest = { contextMenuExpanded = false },
                    offset = with(density) {
                        DpOffset(contextMenuOffset.x.toDp(), contextMenuOffset.y.toDp())
                    }
                ) {
                    DropdownMenuItem(
                        text = { androidx.compose.material3.Text(stringResource(Res.string.menu_export_mermaid_svg)) },
                        onClick = {
                            contextMenuExpanded = false
                            val exportData = image.svgExport
                            if (exportData != null) {
                                scope.launch {
                                    runCatching {
                                        withContext(Dispatchers.IO) {
                                            exportSvgToChosenDirectory(exportData)
                                        }
                                    }.onSuccess { exportedPath ->
                                        if (exportedPath != null) {
                                            userMessageSink.showShort(
                                                getString(Res.string.msg_mermaid_svg_export_success_fmt, exportedPath)
                                            )
                                        }
                                    }.onFailure { error ->
                                        userMessageSink.showShort(
                                            getString(
                                                Res.string.msg_mermaid_svg_export_failure_fmt,
                                                error.message ?: error::class.simpleName.orEmpty()
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(DefaultAppBarButtonSize)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(Res.string.action_cancel),
                tint = contentColor
            )
        }
    }
}

private fun fitImageIntoViewport(
    viewportSize: Size,
    imageAspectRatio: Float
): Size {
    if (viewportSize.width <= 0f || viewportSize.height <= 0f || imageAspectRatio <= 0f) {
        return Size.Zero
    }

    val viewportAspectRatio = viewportSize.width / viewportSize.height
    return if (viewportAspectRatio > imageAspectRatio) {
        Size(
            width = viewportSize.height * imageAspectRatio,
            height = viewportSize.height
        )
    } else {
        Size(
            width = viewportSize.width,
            height = viewportSize.width / imageAspectRatio
        )
    }
}

private fun calculateMaxOffset(
    viewportSize: Size,
    contentSize: Size,
    scale: Float
): Offset {
    val scaledWidth = contentSize.width * scale
    val scaledHeight = contentSize.height * scale
    val maxX = if (scaledWidth > viewportSize.width) {
        (scaledWidth - viewportSize.width) / 2f
    } else {
        0f
    }
    val maxY = if (scaledHeight > viewportSize.height) {
        (scaledHeight - viewportSize.height) / 2f
    } else {
        0f
    }
    return Offset(maxX, maxY)
}

private fun Offset.clamp(maxOffset: Offset): Offset = Offset(
    x = x.coerceIn(-maxOffset.x, maxOffset.x),
    y = y.coerceIn(-maxOffset.y, maxOffset.y)
)

private fun exportSvgToChosenDirectory(exportData: ImagePreviewSvgExportData): String? {
    val directoryPath = chooseDirectoryPath() ?: return null
    val directory = File(directoryPath)
    if (!directory.exists() && !directory.mkdirs()) {
        error("Failed to create export directory: $directoryPath")
    }
    val targetFile = resolveSvgExportFile(directory, exportData.suggestedFileName)
    targetFile.writeText(exportData.svgText, StandardCharsets.UTF_8)
    return targetFile.absolutePath
}

private fun resolveSvgExportFile(directory: File, suggestedFileName: String): File {
    val normalizedName = suggestedFileName
        .ifBlank { "archivex-mermaid.svg" }
        .let { if (it.endsWith(".svg", ignoreCase = true)) it else "$it.svg" }
    val baseName = normalizedName.removeSuffix(".svg")
    var candidate = File(directory, "$baseName.svg")
    var index = 2
    while (candidate.exists()) {
        candidate = File(directory, "$baseName-$index.svg")
        index += 1
    }
    return candidate
}

private fun chooseDirectoryPath(): String? {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isMultiSelectionEnabled = false
    }
    return showChooser(chooser)
}

private fun showChooser(chooser: JFileChooser): String? {
    var selectedPath: String? = null
    runOnEdtAndWait {
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            selectedPath = chooser.selectedFile?.absolutePath
        }
    }
    return selectedPath
}

private fun runOnEdtAndWait(action: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) {
        action()
    } else {
        SwingUtilities.invokeAndWait(action)
    }
}
