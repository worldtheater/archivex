package com.worldtheater.archive.feature.settings.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import archivex.composeapp.generated.resources.*
import com.worldtheater.archive.domain.model.MAX_FOLDER_DIRECT_ITEMS
import com.worldtheater.archive.domain.model.MAX_NOTE_BODY_LENGTH
import com.worldtheater.archive.domain.model.MAX_TOTAL_ITEMS
import com.worldtheater.archive.domain.model.NoteLimitException
import com.worldtheater.archive.domain.model.NoteLimitType
import com.worldtheater.archive.feature.debug.DebugContentStyle
import com.worldtheater.archive.feature.debug.DebugDataGenerator
import com.worldtheater.archive.feature.note_list.screens.NotePreviewContent
import com.worldtheater.archive.platform.system.UserMessageSink
import com.worldtheater.archive.ui.theme.rememberContentTopPadding
import com.worldtheater.archive.ui.widget.SettingsAppTopBar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun DebugToolsScreen(
    onBack: () -> Unit
) {
    val debugDataGenerator: DebugDataGenerator = koinInject()
    val userMessageSink: UserMessageSink = koinInject()
    val title = stringResource(Res.string.debug_tools_title)
    val invalidInput = stringResource(Res.string.debug_invalid_input)
    val totalItemsLimit = stringResource(Res.string.msg_total_items_limit_fmt, MAX_TOTAL_ITEMS)
    val noteBodyTooLong = stringResource(Res.string.msg_note_body_too_long_fmt, MAX_NOTE_BODY_LENGTH)
    val folderItemsLimit = stringResource(Res.string.msg_folder_items_limit_fmt, MAX_FOLDER_DIRECT_ITEMS)
    val contentStylePlain = stringResource(Res.string.debug_content_style_plain)
    val contentStyleChipPhrases = stringResource(Res.string.debug_content_style_chip_phrases)
    val optionOn = stringResource(Res.string.debug_option_on)
    val optionOff = stringResource(Res.string.debug_option_off)
    val defaultGenerateNotesButton = stringResource(Res.string.debug_action_generate_notes)
    val defaultGenerateFolderChildrenButton = stringResource(Res.string.debug_action_generate_folder_children)
    val defaultGenerateBenchmarkButton = stringResource(Res.string.debug_action_generate_benchmark)
    val openPreviewReproButton = "Open Preview Repro"
    val openStaticCacheReproButton = "Open Static Cache Repro"
    val openStaticSmallCacheReproButton = "Open Static Small-Image Repro"
    val exportMermaidCacheButton = "Export Mermaid Cache"
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val contentTopPadding = rememberContentTopPadding()
    val previewReproScrollState = rememberScrollState()
    val cacheImageLoader = rememberDebugMermaidCacheImageLoader()
    var noteCountInput by rememberSaveable { mutableStateOf("100") }
    var bodyLengthInput by rememberSaveable { mutableStateOf("128") }
    var folderChildCountInput by rememberSaveable { mutableStateOf("300") }
    var contentStyle by rememberSaveable { mutableStateOf(DebugContentStyle.PLAIN_TEXT) }
    var includeTitle by rememberSaveable { mutableStateOf(true) }
    var isGeneratingNotes by rememberSaveable { mutableStateOf(false) }
    var isGeneratingFolderChildren by rememberSaveable { mutableStateOf(false) }
    var isGeneratingBenchmarkDataset by rememberSaveable { mutableStateOf(false) }
    var noteProgress by rememberSaveable { mutableStateOf(0) }
    var noteProgressTotal by rememberSaveable { mutableStateOf(0) }
    var folderProgress by rememberSaveable { mutableStateOf(0) }
    var folderProgressTotal by rememberSaveable { mutableStateOf(0) }
    var benchmarkProgress by rememberSaveable { mutableStateOf(0) }
    var benchmarkProgressTotal by rememberSaveable { mutableStateOf(0) }
    var showPreviewRepro by rememberSaveable { mutableStateOf(false) }
    var showStaticCacheRepro by rememberSaveable { mutableStateOf(false) }
    var showStaticSmallCacheRepro by rememberSaveable { mutableStateOf(false) }
    var staticCacheImages by remember { mutableStateOf<List<DebugMermaidCacheImage>>(emptyList()) }

    LaunchedEffect(showStaticCacheRepro, showStaticSmallCacheRepro) {
        if (showStaticCacheRepro || showStaticSmallCacheRepro) {
            val limit = if (showStaticSmallCacheRepro) 3 else 5
            staticCacheImages = cacheImageLoader.loadRecentImages(limit = limit)
        }
    }

    if (showPreviewRepro) {
        Box(modifier = Modifier.fillMaxSize()) {
            NotePreviewContent(
                title = "Markdown Preview Repro",
                metadataText = "Uses the same Mermaid samples that reproduce the iOS fling issue.",
                scrollState = previewReproScrollState,
                contentTopPadding = contentTopPadding,
                showMarkdown = true,
                isLongNote = true,
                markdown = IOS_FLING_REPRO_MARKDOWN,
                onMermaidImageClick = null,
                modifier = Modifier.fillMaxSize()
            )

            SettingsAppTopBar(
                title = "Preview Repro",
                onBack = { showPreviewRepro = false },
                listState = listState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        return
    }

    if (showStaticCacheRepro || showStaticSmallCacheRepro) {
        val targetImageWidthDp = if (showStaticSmallCacheRepro) 180f else 320f
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = contentTopPadding,
                    bottom = 24.dp
                )
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = if (showStaticSmallCacheRepro) {
                                "Static Small-Image Cache Repro"
                            } else {
                                "Static Mermaid Cache Repro"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (showStaticSmallCacheRepro) {
                                "Displays the same recent Mermaid PNG files from disk cache, but scaled down to smaller display heights."
                            } else {
                                "Displays recent Mermaid PNG files loaded directly from disk cache, without dynamic Mermaid rendering."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                if (staticCacheImages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                } else {
                    items(staticCacheImages, key = { it.id }) { image ->
                        val decodedImage by produceState<DebugMermaidCacheDecodedImage?>(
                            initialValue = null,
                            key1 = image.path
                        ) {
                            value = cacheImageLoader.decodeImage(image.path)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = image.id,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (decodedImage == null) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(top = 16.dp)
                                        .size(24.dp)
                                )
                            } else {
                                val aspectRatio = if (decodedImage!!.widthPx > 0 && decodedImage!!.heightPx > 0) {
                                    decodedImage!!.heightPx.toFloat() / decodedImage!!.widthPx.toFloat()
                                } else {
                                    1f
                                }
                                Image(
                                    bitmap = decodedImage!!.bitmap,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(top = 8.dp)
                                        .width(targetImageWidthDp.dp)
                                        .height((targetImageWidthDp * aspectRatio).dp)
                                )
                                Text(
                                    text = "size=${decodedImage!!.widthPx}x${decodedImage!!.heightPx}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            SettingsAppTopBar(
                title = if (showStaticSmallCacheRepro) "Static Small Repro" else "Static Cache Repro",
                onBack = {
                    showStaticCacheRepro = false
                    showStaticSmallCacheRepro = false
                },
                listState = listState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        return
    }

    fun parsePositiveInt(raw: String): Int? = raw.trim().toIntOrNull()?.takeIf { it > 0 }

    fun showLimitMessage(limitException: NoteLimitException?) {
        if (limitException == null) return
        val message = when (limitException.type) {
            NoteLimitType.TOTAL_ITEMS -> totalItemsLimit
            NoteLimitType.NOTE_BODY_LENGTH -> noteBodyTooLong
            NoteLimitType.FOLDER_DIRECT_ITEMS -> folderItemsLimit
        }
        userMessageSink.showShort(message)
    }

    val contentStyleSubtitle = when (contentStyle) {
        DebugContentStyle.PLAIN_TEXT -> contentStylePlain
        DebugContentStyle.CHIP_PHRASES -> contentStyleChipPhrases
    }
    val includeTitleSubtitle = if (includeTitle) optionOn else optionOff
    val generateNotesButtonText = if (isGeneratingNotes) {
        stringResource(Res.string.debug_generating_progress_fmt, noteProgress, noteProgressTotal)
    } else {
        defaultGenerateNotesButton
    }
    val generateFolderChildrenButtonText = if (isGeneratingFolderChildren) {
        stringResource(Res.string.debug_generating_progress_fmt, folderProgress, folderProgressTotal)
    } else {
        defaultGenerateFolderChildrenButton
    }
    val generateBenchmarkButtonText = if (isGeneratingBenchmarkDataset) {
        stringResource(Res.string.debug_generating_progress_fmt, benchmarkProgress, benchmarkProgressTotal)
    } else {
        defaultGenerateBenchmarkButton
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DebugToolsContent(
            listState = listState,
            contentTopPadding = contentTopPadding,
            openPreviewReproButton = openPreviewReproButton,
            openStaticCacheReproButton = openStaticCacheReproButton,
            openStaticSmallCacheReproButton = openStaticSmallCacheReproButton,
            exportMermaidCacheButton = exportMermaidCacheButton,
            contentStyleSubtitle = contentStyleSubtitle,
            includeTitleSubtitle = includeTitleSubtitle,
            generateNotesButton = generateNotesButtonText,
            generateFolderChildrenButton = generateFolderChildrenButtonText,
            generateBenchmarkButton = generateBenchmarkButtonText,
            noteCountInput = noteCountInput,
            onNoteCountChange = { noteCountInput = it },
            bodyLengthInput = bodyLengthInput,
            onBodyLengthChange = { bodyLengthInput = it },
            folderChildCountInput = folderChildCountInput,
            onFolderChildCountChange = { folderChildCountInput = it },
            includeTitle = includeTitle,
            onIncludeTitleChange = { includeTitle = it },
            onToggleContentStyle = {
                contentStyle = when (contentStyle) {
                    DebugContentStyle.PLAIN_TEXT -> DebugContentStyle.CHIP_PHRASES
                    DebugContentStyle.CHIP_PHRASES -> DebugContentStyle.PLAIN_TEXT
                }
            },
            isGeneratingNotes = isGeneratingNotes,
            isGeneratingFolderChildren = isGeneratingFolderChildren,
            isGeneratingBenchmarkDataset = isGeneratingBenchmarkDataset,
            onOpenPreviewRepro = { showPreviewRepro = true },
            onOpenStaticCacheRepro = { showStaticCacheRepro = true },
            onOpenStaticSmallCacheRepro = { showStaticSmallCacheRepro = true },
            onExportMermaidCache = {
                scope.launch {
                    val result = exportDebugMermaidCacheToDocuments()
                    userMessageSink.showShort(
                        "Exported ${result.exportedFileCount} files to ${result.directoryPath}"
                    )
                }
            },
            onGenerateNotes = {
                val count = parsePositiveInt(noteCountInput)
                val bodyLen = parsePositiveInt(bodyLengthInput)
                if (count == null || bodyLen == null) {
                    userMessageSink.showShort(invalidInput)
                    return@DebugToolsContent
                }
                noteProgress = 0
                noteProgressTotal = count
                isGeneratingNotes = true
                scope.launch {
                    val result = debugDataGenerator.generateRootNotes(
                        count = count,
                        bodyLength = bodyLen,
                        contentStyle = contentStyle,
                        includeTitle = includeTitle,
                        onProgress = { created, total ->
                            noteProgress = created
                            noteProgressTotal = total
                        }
                    )
                    isGeneratingNotes = false
                    if (result.createdCount > 0) {
                        userMessageSink.showShort(getString(Res.string.debug_created_notes_fmt, result.createdCount))
                    }
                    showLimitMessage(result.limitException)
                }
            },
            onGenerateFolderChildren = {
                val childCount = parsePositiveInt(folderChildCountInput)
                val bodyLen = parsePositiveInt(bodyLengthInput)
                if (childCount == null || bodyLen == null) {
                    userMessageSink.showShort(invalidInput)
                    return@DebugToolsContent
                }
                folderProgress = 0
                folderProgressTotal = childCount
                isGeneratingFolderChildren = true
                scope.launch {
                    val result = debugDataGenerator.generateFolderWithChildren(
                        childCount = childCount,
                        bodyLength = bodyLen,
                        contentStyle = contentStyle,
                        includeTitle = includeTitle,
                        onProgress = { created, total ->
                            folderProgress = created
                            folderProgressTotal = total
                        }
                    )
                    isGeneratingFolderChildren = false
                    userMessageSink.showShort(
                        getString(Res.string.debug_created_folder_children_fmt, result.createdCount)
                    )
                    showLimitMessage(result.limitException)
                }
            },
            onGenerateBenchmarkDataset = {
                benchmarkProgress = 0
                benchmarkProgressTotal = MAX_TOTAL_ITEMS
                isGeneratingBenchmarkDataset = true
                scope.launch {
                    val result = debugDataGenerator.generateBenchmarkDataset(
                        MAX_TOTAL_ITEMS,
                        onProgress = { created, total ->
                            benchmarkProgress = created
                            benchmarkProgressTotal = total
                        }
                    )
                    isGeneratingBenchmarkDataset = false
                    userMessageSink.showShort(getString(Res.string.debug_created_notes_fmt, result.createdCount))
                    showLimitMessage(result.limitException)
                }
            }
        )

        SettingsAppTopBar(
            title = title,
            onBack = onBack,
            listState = listState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

private val IOS_FLING_REPRO_MARKDOWN = """
1. 类图（Class Diagram）

```mermaid
classDiagram
Animal <|-- Dog
Animal <|-- Cat
class Animal{
  +sleep()
}
class Dog{
  +bark()
}
class Cat{
  +meow()
}
```

4. 甘特图（Gantt）

表示一个简单项目的任务时间安排。

```mermaid
gantt
title 项目计划
dateFormat  YYYY-MM-DD
section 设计
需求分析           :done,    des1, 2026-01-01,2026-01-04
交互设计           :active,  des2, 2026-01-05, 3d
section 开发
功能开发           :         dev1, after des2, 6d
联调修复           :         dev2, after dev1, 3d
section 发布
灰度发布           :         rel1, after dev2, 2d
```

5. 状态图（State Diagram）

描述订单从创建到完成的状态变化。

```mermaid
stateDiagram-v2
    [*] --> 待支付
    待支付 --> 已支付 : 用户完成支付
    待支付 --> 已取消 : 用户取消订单
    已支付 --> 待发货 : 系统确认收款
    待发货 --> 已发货 : 商家发货
    已发货 --> 已完成 : 用户确认收货
    已取消 --> [*]
    已完成 --> [*]
```

6. 再次测试普通文本区域

从这里开始上划和下划，观察 iOS 是否还能稳定触发 fling。

继续滚动到上面的 Mermaid 图边缘和下面的 Mermaid 图顶部，再分别测试一次。
""".trimIndent()
