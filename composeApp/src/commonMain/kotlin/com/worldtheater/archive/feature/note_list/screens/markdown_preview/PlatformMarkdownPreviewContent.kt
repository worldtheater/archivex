package com.worldtheater.archive.feature.note_list.screens.markdown_preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.model.rememberMarkdownState
import com.worldtheater.archive.platform.system.previewMermaidMaxDisplayHeightDp
import com.worldtheater.archive.ui.theme.isAppInDarkTheme
import com.worldtheater.archive.ui.widget.previewMarkdownPadding
import com.worldtheater.archive.ui.widget.previewMarkdownTypography

internal interface PlatformMarkdownPreviewRuntime {
    suspend fun loadMermaidScript(): String

    suspend fun decodePreviewDiagram(
        payload: MermaidSnapshotCachedDiagramPayload,
        maxDisplayWidthPx: Int,
        maxDisplayHeightPx: Int
    ): MermaidSnapshotDiagram?

    suspend fun renderDiagram(
        mermaidCode: String,
        themeConfig: String,
        renderWidthPx: Int,
        maxDisplayHeightPx: Int,
        script: String
    ): MermaidSnapshotDiagram

    fun dispose() = Unit
}

internal object MermaidPreviewDefaults {
    const val CACHE_VERSION: Int = 6
    const val DEFAULT_HEIGHT_DP: Int = 240
    const val MIN_HEIGHT_DP: Int = 180
    const val MAX_RASTER_WIDTH_PX: Int = 1200
    const val MAX_RASTER_HEIGHT_PX: Int = 2400
    const val MAX_RASTER_PIXELS: Int = 4_000_000
    const val MAX_DEVICE_PIXEL_RATIO: Double = 1.5
}

@Composable
internal expect fun rememberPlatformMarkdownPreviewRuntime(): PlatformMarkdownPreviewRuntime

internal data class MermaidSnapshotCacheKey(
    val cacheVersion: Int,
    val mermaidCode: String,
    val themeConfig: String,
    val renderWidthPx: Int
)

internal data class MermaidSnapshotDiagram(
    val bitmap: ImageBitmap,
    val widthDp: Int,
    val heightDp: Int,
    val pngBytes: ByteArray
)

private data class MermaidScriptLoadState(
    val script: String? = null,
    val loadFailed: Boolean = false,
)

private sealed interface PreviewBlock {
    data class Markdown(val text: String) : PreviewBlock
    data class Mermaid(val code: String) : PreviewBlock
}

@Composable
fun PlatformMarkdownPreviewContent(
    markdown: String,
    onMermaidImageClick: ((ImageBitmap, Int, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val runtime = rememberPlatformMarkdownPreviewRuntime()
    val cacheIo = rememberPlatformMermaidSnapshotCacheIo()
    val diagramCache = remember(cacheIo) { MermaidSnapshotDiagramCache(cacheIo) }
    val scriptState by produceState(initialValue = MermaidScriptLoadState(), runtime) {
        val loadedScript = runCatching { runtime.loadMermaidScript() }.getOrNull()
        value = MermaidScriptLoadState(
            script = loadedScript,
            loadFailed = loadedScript == null
        )
    }
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = isAppInDarkTheme()
    val mermaidThemeConfig = remember(colorScheme, isDarkTheme) {
        buildSharedMermaidThemeConfig(
            isDarkTheme = isDarkTheme,
            background = colorScheme.background.toCssHex(),
            surface = colorScheme.surface.toCssHex(),
            surfaceVariant = colorScheme.surfaceVariant.toCssHex(),
            primary = colorScheme.primary.toCssHex(),
            secondary = colorScheme.secondary.toCssHex(),
            onSurface = colorScheme.onSurface.toCssHex(),
            onSurfaceVariant = colorScheme.onSurfaceVariant.toCssHex(),
            onPrimary = colorScheme.onPrimary.toCssHex(),
            activationBkgColor = colorScheme.primary.copy(alpha = if (isDarkTheme) 0.28f else 0.18f).toCssHex(),
            outline = colorScheme.onSurfaceVariant.copy(alpha = if (isDarkTheme) 0.6f else 0.45f).toCssHex(),
            themeName = if (isDarkTheme) "dark" else "default"
        )
    }
    val previewBlocks = remember(markdown) {
        splitPreviewBlocks(markdown).map { block ->
            when (block) {
                is PreviewBlock.Markdown -> PreviewBlock.Markdown(normalizeMarkdownForPreview(block.text))
                is PreviewBlock.Mermaid -> block
            }
        }
    }

    DisposableEffect(runtime) {
        onDispose { runtime.dispose() }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        previewBlocks.forEachIndexed { index, block ->
            key(index) {
                when (block) {
                    is PreviewBlock.Markdown -> {
                        if (block.text.isNotBlank()) {
                            val markdownState = rememberMarkdownState(block.text, immediate = false)
                            Markdown(
                                markdownState = markdownState,
                                modifier = Modifier.fillMaxWidth(),
                                colors = markdownColor(),
                                typography = previewMarkdownTypography(),
                                padding = previewMarkdownPadding(),
                                loading = {}
                            )
                        }
                    }

                    is PreviewBlock.Mermaid -> {
                        MermaidPreviewImage(
                            mermaidCode = block.code,
                            themeConfig = mermaidThemeConfig,
                            onImageClick = onMermaidImageClick,
                            scriptState = scriptState,
                            loadCachedDiagram = { cacheKey, maxDisplayWidthPx, maxDisplayHeightPx ->
                                val payload = diagramCache.get(cacheKey) ?: return@MermaidPreviewImage null
                                runtime.decodePreviewDiagram(
                                    payload = payload,
                                    maxDisplayWidthPx = maxDisplayWidthPx,
                                    maxDisplayHeightPx = maxDisplayHeightPx
                                )
                            },
                            saveRenderedDiagram = { cacheKey, rendered ->
                                diagramCache.put(cacheKey, rendered.toCachedPayload())
                            },
                            renderDiagram = { normalizedCode, currentThemeConfig, renderWidthPx, maxDisplayHeightPx, script ->
                                runtime.renderDiagram(
                                    mermaidCode = normalizedCode,
                                    themeConfig = currentThemeConfig,
                                    renderWidthPx = renderWidthPx,
                                    maxDisplayHeightPx = maxDisplayHeightPx,
                                    script = script
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MermaidPreviewImage(
    mermaidCode: String,
    themeConfig: String,
    modifier: Modifier = Modifier,
    onImageClick: ((ImageBitmap, Int, Int) -> Unit)? = null,
    scriptState: MermaidScriptLoadState,
    loadCachedDiagram: suspend (MermaidSnapshotCacheKey, Int, Int) -> MermaidSnapshotDiagram?,
    saveRenderedDiagram: suspend (MermaidSnapshotCacheKey, MermaidSnapshotDiagram) -> Unit = { _, _ -> },
    renderDiagram: suspend (
        normalizedMermaidCode: String,
        themeConfig: String,
        renderWidthPx: Int,
        maxDisplayHeightPx: Int,
        script: String
    ) -> MermaidSnapshotDiagram,
) {
    val density = LocalDensity.current
    val maxDisplayHeight = previewMermaidMaxDisplayHeightDp().dp
    val normalizedMermaidCode = remember(mermaidCode) { normalizeMermaidForPreview(mermaidCode) }
    var contentHeightDp by remember(mermaidCode) { mutableIntStateOf(MermaidPreviewDefaults.DEFAULT_HEIGHT_DP) }
    var renderedDiagram by remember(mermaidCode) { mutableStateOf<MermaidSnapshotDiagram?>(null) }
    var renderError by remember(mermaidCode) { mutableStateOf<String?>(null) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val renderWidthPx = remember(maxWidth, density) {
            with(density) { maxWidth.roundToPx().coerceAtLeast(1) }
        }
        val maxDisplayHeightPx = remember(maxDisplayHeight, density) {
            with(density) { maxDisplayHeight.roundToPx().coerceAtLeast(1) }
        }
        val cacheKey = remember(normalizedMermaidCode, themeConfig, renderWidthPx) {
            MermaidSnapshotCacheKey(
                cacheVersion = MermaidPreviewDefaults.CACHE_VERSION,
                mermaidCode = normalizedMermaidCode,
                themeConfig = themeConfig,
                renderWidthPx = renderWidthPx
            )
        }

        LaunchedEffect(cacheKey, scriptState) {
            renderError = null

            val script = scriptState.script
            if (script == null) {
                if (renderedDiagram == null) {
                    renderError = if (scriptState.loadFailed) "Missing Mermaid script" else null
                    contentHeightDp = MermaidPreviewDefaults.DEFAULT_HEIGHT_DP
                }
                return@LaunchedEffect
            }

            val cachedDiagram = loadCachedDiagram(cacheKey, renderWidthPx, maxDisplayHeightPx)
            if (cachedDiagram != null) {
                renderedDiagram = cachedDiagram
                contentHeightDp = cachedDiagram.heightDp.coerceAtLeast(1)
                return@LaunchedEffect
            }

            runCatching {
                renderDiagram(normalizedMermaidCode, themeConfig, renderWidthPx, maxDisplayHeightPx, script)
            }.onSuccess { rendered ->
                saveRenderedDiagram(cacheKey, rendered)
                renderedDiagram = rendered
                contentHeightDp = rendered.heightDp.coerceAtLeast(1)
            }.onFailure { error ->
                if (renderedDiagram == null) {
                    renderError = error.message ?: "Mermaid render failed"
                    contentHeightDp = MermaidPreviewDefaults.DEFAULT_HEIGHT_DP
                }
            }
        }

        val maxAvailableWidth = this.maxWidth
        val renderedDisplaySize = renderedDiagram?.let { diagram ->
            val widthScale = if (diagram.widthDp > 0) {
                maxAvailableWidth.value / diagram.widthDp.toFloat()
            } else {
                1f
            }
            val heightScale = if (diagram.heightDp > 0) {
                maxDisplayHeight.value / diagram.heightDp.toFloat()
            } else {
                1f
            }
            val scale = minOf(1f, widthScale, heightScale)
            (diagram.widthDp * scale).dp to (diagram.heightDp * scale).dp
        }
        val contentHeight = (renderedDisplaySize?.second ?: contentHeightDp.dp)
            .coerceAtLeast(MermaidPreviewDefaults.MIN_HEIGHT_DP.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight)
        ) {
            when {
                renderedDiagram != null -> {
                    val diagram = renderedDiagram!!
                    val (displayWidth, displayHeight) = renderedDisplaySize ?: (maxAvailableWidth to contentHeight)
                    val imageModifier = Modifier
                        .align(Alignment.TopCenter)
                        .width(displayWidth)
                        .height(displayHeight)
                        .clickable(enabled = onImageClick != null) {
                            onImageClick?.invoke(diagram.bitmap, diagram.widthDp, diagram.heightDp)
                        }
                    Image(
                        bitmap = diagram.bitmap,
                        contentDescription = null,
                        modifier = imageModifier
                    )
                }

                renderError != null -> {
                    Text(
                        text = "Mermaid render error: ${renderError.orEmpty()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }

                else -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

private val OPENING_PUNCTUATION = setOf('“', '"', '\'', '‘', '（', '(', '[', '【')
private val ASCII_QUOTE = '"'
private val CJK_OPEN_QUOTE = '“'
private val CJK_CLOSE_QUOTE = '”'
private const val WORD_JOINER = '\u2060'

private fun normalizeMarkdownForPreview(rawText: String): String {
    return rawText
        .replace("\n", "  \n")
        .let(::normalizeStrongOpeningPunctuation)
        .let { text -> normalizeQuotedStrong(text, ASCII_QUOTE, ASCII_QUOTE) }
        .let { text -> normalizeQuotedStrong(text, CJK_OPEN_QUOTE, CJK_CLOSE_QUOTE) }
        .let(::normalizeStrongClosingPunctuation)
}

private fun normalizeStrongOpeningPunctuation(text: String): String {
    val result = StringBuilder(text.length + 8)
    var index = 0
    while (index < text.length) {
        if (index + 2 < text.length &&
            text[index] == '*' &&
            text[index + 1] == '*' &&
            text[index + 2] in OPENING_PUNCTUATION
        ) {
            result.append("**").append(WORD_JOINER)
            index += 2
            continue
        }
        result.append(text[index])
        index += 1
    }
    return result.toString()
}

private fun normalizeQuotedStrong(text: String, openingQuote: Char, closingQuote: Char): String {
    val result = StringBuilder(text.length)
    var index = 0
    while (index < text.length) {
        if (index + 4 < text.length &&
            text[index] == '*' &&
            text[index + 1] == '*' &&
            text[index + 2] == openingQuote
        ) {
            val quoteStart = index + 3
            val quoteEnd = text.indexOf(closingQuote, quoteStart)
            if (quoteEnd > quoteStart &&
                quoteEnd + 2 < text.length &&
                text[quoteEnd + 1] == '*' &&
                text[quoteEnd + 2] == '*'
            ) {
                var containsLineBreak = false
                var scan = quoteStart
                while (scan < quoteEnd) {
                    if (text[scan] == '\n') {
                        containsLineBreak = true
                        break
                    }
                    scan += 1
                }
                if (!containsLineBreak) {
                    result.append(openingQuote)
                    result.append("**")
                    result.append(text, quoteStart, quoteEnd)
                    result.append("**")
                    result.append(closingQuote)
                    index = quoteEnd + 3
                    continue
                }
            }
        }
        result.append(text[index])
        index += 1
    }
    return result.toString()
}

private fun normalizeStrongClosingPunctuation(text: String): String {
    val result = StringBuilder(text.length + 8)
    var index = 0
    while (index < text.length) {
        if (index + 3 < text.length &&
            isClosingPunctuation(text[index]) &&
            text[index + 1] == '*' &&
            text[index + 2] == '*' &&
            text[index + 3].isLetter()
        ) {
            result.append(text[index]).append(WORD_JOINER).append("**")
            index += 3
            continue
        }
        result.append(text[index])
        index += 1
    }
    return result.toString()
}

private fun isClosingPunctuation(char: Char): Boolean {
    return !char.isLetterOrDigit() && !char.isWhitespace()
}

private fun splitPreviewBlocks(markdown: String): List<PreviewBlock> {
    if (markdown.isEmpty()) return listOf(PreviewBlock.Markdown(""))

    val lines = markdown.lines()
    val blocks = mutableListOf<PreviewBlock>()
    val markdownBuffer = StringBuilder()
    var index = 0

    fun flushMarkdownBuffer() {
        if (markdownBuffer.isNotEmpty()) {
            blocks += PreviewBlock.Markdown(markdownBuffer.toString())
            markdownBuffer.clear()
        }
    }

    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trim()
        val fenceType = if (trimmed.startsWith("```")) trimmed.removePrefix("```").trim() else null
        val isMermaidFenceStart = fenceType.equals("mermaid", ignoreCase = true)

        if (!isMermaidFenceStart) {
            markdownBuffer.append(line)
            if (index != lines.lastIndex) markdownBuffer.append('\n')
            index += 1
            continue
        }

        flushMarkdownBuffer()
        index += 1

        val fencedBuffer = StringBuilder()
        var closed = false
        while (index < lines.size) {
            val current = lines[index]
            if (current.trim() == "```") {
                closed = true
                index += 1
                break
            }
            fencedBuffer.append(current)
            if (index != lines.lastIndex) fencedBuffer.append('\n')
            index += 1
        }

        if (closed) {
            blocks += PreviewBlock.Mermaid(fencedBuffer.toString().trimEnd('\n', '\r'))
        } else {
            markdownBuffer.append("```")
            markdownBuffer.append(fenceType.orEmpty())
            markdownBuffer.append('\n')
            markdownBuffer.append(fencedBuffer)
        }
    }

    flushMarkdownBuffer()
    return if (blocks.isEmpty()) listOf(PreviewBlock.Markdown("")) else blocks
}

private fun normalizeMermaidForPreview(source: String): String {
    val trimmedStart = source.trimStart()
    val isFlowchart = trimmedStart.startsWith("flowchart", ignoreCase = true) ||
        trimmedStart.startsWith("graph", ignoreCase = true)
    if (!isFlowchart) return source

    val normalizedSource = source
        .replace("\\\\n", "<br/>")
        .replace("\\n", "<br/>")
    val nodePattern = Regex("""(\b[A-Za-z0-9_]+)\[([^\[\]\"]*[()]+[^\[\]\"]*)]""")
    val quotedLabelPattern = Regex("\"((?:\\\\.|[^\"\\\\])*)\"")
    return normalizedSource.lineSequence().joinToString("\n") { rawLine ->
        val lineWithQuotedBreaks = quotedLabelPattern.replace(rawLine) { match ->
            "\"${match.groupValues[1]}\""
        }
        nodePattern.replace(lineWithQuotedBreaks) { match ->
            val nodeId = match.groupValues[1]
            val label = match.groupValues[2]
            val escapedLabel = buildString(label.length + 8) {
                label.forEach { char ->
                    if (char == '"') append("\\\"") else append(char)
                }
            }
            "$nodeId[\"$escapedLabel\"]"
        }
    }
}
