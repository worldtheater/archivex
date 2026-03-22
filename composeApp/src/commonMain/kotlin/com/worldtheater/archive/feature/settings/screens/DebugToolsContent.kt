package com.worldtheater.archive.feature.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import archivex.composeapp.generated.resources.*
import com.worldtheater.archive.feature.settings.components.*
import com.worldtheater.archive.domain.model.MAX_FOLDER_DIRECT_ITEMS
import com.worldtheater.archive.domain.model.MAX_NOTE_BODY_LENGTH
import com.worldtheater.archive.domain.model.MAX_TOTAL_ITEMS
import com.worldtheater.archive.ui.shape.SmoothRoundedCornerShape
import org.jetbrains.compose.resources.stringResource

@Composable
fun DebugToolsContent(
    listState: LazyListState,
    contentTopPadding: Dp,
    openPreviewReproButton: String,
    openStaticCacheReproButton: String,
    openStaticSmallCacheReproButton: String,
    exportMermaidCacheButton: String,
    contentStyleSubtitle: String,
    includeTitleSubtitle: String,
    generateNotesButton: String,
    generateFolderChildrenButton: String,
    generateBenchmarkButton: String,
    noteCountInput: String,
    onNoteCountChange: (String) -> Unit,
    bodyLengthInput: String,
    onBodyLengthChange: (String) -> Unit,
    folderChildCountInput: String,
    onFolderChildCountChange: (String) -> Unit,
    includeTitle: Boolean,
    onIncludeTitleChange: (Boolean) -> Unit,
    onToggleContentStyle: () -> Unit,
    isGeneratingNotes: Boolean,
    isGeneratingFolderChildren: Boolean,
    isGeneratingBenchmarkDataset: Boolean,
    onOpenPreviewRepro: () -> Unit,
    onOpenStaticCacheRepro: () -> Unit,
    onOpenStaticSmallCacheRepro: () -> Unit,
    onExportMermaidCache: () -> Unit,
    onGenerateNotes: () -> Unit,
    onGenerateFolderChildren: () -> Unit,
    onGenerateBenchmarkDataset: () -> Unit
) {
    val limitSummary = stringResource(
        Res.string.debug_limit_summary_fmt,
        MAX_TOTAL_ITEMS,
        MAX_NOTE_BODY_LENGTH,
        MAX_FOLDER_DIRECT_ITEMS
    )
    val contentStyleTitle = stringResource(Res.string.debug_content_style_title)
    val includeTitleTitle = stringResource(Res.string.debug_option_add_title)
    val noteCountTitle = stringResource(Res.string.debug_input_note_count)
    val bodyLengthTitle = stringResource(Res.string.debug_input_body_length)
    val generateNotesDesc = stringResource(Res.string.debug_action_generate_notes_desc)
    val folderChildrenCountTitle = stringResource(Res.string.debug_input_folder_children_count)
    val generateFolderChildrenDesc = stringResource(Res.string.debug_action_generate_folder_children_desc)
    val generateBenchmarkDesc = stringResource(Res.string.debug_action_generate_benchmark_desc)

    Scaffold { paddingValues ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = contentTopPadding,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = SETTINGS_SECTION_HORIZONTAL_PADDING)
                        .fillMaxWidth(),
                    shape = SmoothRoundedCornerShape(radius = SETTINGS_SECTION_CARD_RADIUS),
                    color = settingsSectionCardColor()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Open the isolated markdown preview repro page using the same Mermaid samples that trigger the iOS fling issue.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        DebugActionButton(
                            text = openPreviewReproButton,
                            enabled = true,
                            onClick = onOpenPreviewRepro,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        DebugActionButton(
                            text = openStaticCacheReproButton,
                            enabled = true,
                            onClick = onOpenStaticCacheRepro,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        DebugActionButton(
                            text = openStaticSmallCacheReproButton,
                            enabled = true,
                            onClick = onOpenStaticSmallCacheRepro,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        DebugActionButton(
                            text = exportMermaidCacheButton,
                            enabled = true,
                            onClick = onExportMermaidCache,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = SETTINGS_SECTION_HORIZONTAL_PADDING)
                        .fillMaxWidth(),
                    shape = SmoothRoundedCornerShape(radius = SETTINGS_SECTION_CARD_RADIUS),
                    color = settingsSectionCardColor()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = limitSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SettingsItem(
                            title = contentStyleTitle,
                            subtitle = contentStyleSubtitle,
                            onClick = onToggleContentStyle
                        )
                        Spacer(modifier = Modifier.height(SETTINGS_SECTION_ITEM_SPACING))
                        SettingsItem(
                            title = includeTitleTitle,
                            subtitle = includeTitleSubtitle,
                            action = {
                                Switch(
                                    checked = includeTitle,
                                    onCheckedChange = onIncludeTitleChange
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(SETTINGS_SECTION_ITEM_SPACING))

                        DebugNumberField(
                            title = noteCountTitle,
                            value = noteCountInput,
                            onValueChange = onNoteCountChange
                        )
                        Spacer(modifier = Modifier.height(SETTINGS_SECTION_ITEM_SPACING))
                        DebugNumberField(
                            title = bodyLengthTitle,
                            value = bodyLengthInput,
                            onValueChange = onBodyLengthChange
                        )
                        Spacer(modifier = Modifier.height(SETTINGS_SECTION_ITEM_SPACING))
                        Text(
                            text = generateNotesDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        DebugActionButton(
                            text = generateNotesButton,
                            enabled = !isGeneratingNotes &&
                                    !isGeneratingFolderChildren &&
                                    !isGeneratingBenchmarkDataset,
                            onClick = onGenerateNotes,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .padding(horizontal = SETTINGS_SECTION_HORIZONTAL_PADDING)
                        .fillMaxWidth(),
                    shape = SmoothRoundedCornerShape(radius = SETTINGS_SECTION_CARD_RADIUS),
                    color = settingsSectionCardColor()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        DebugNumberField(
                            title = folderChildrenCountTitle,
                            value = folderChildCountInput,
                            onValueChange = onFolderChildCountChange
                        )
                        Spacer(modifier = Modifier.height(SETTINGS_SECTION_ITEM_SPACING))
                        DebugNumberField(
                            title = bodyLengthTitle,
                            value = bodyLengthInput,
                            onValueChange = onBodyLengthChange
                        )
                        Spacer(modifier = Modifier.height(SETTINGS_SECTION_ITEM_SPACING))
                        Text(
                            text = generateFolderChildrenDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        DebugActionButton(
                            text = generateFolderChildrenButton,
                            enabled = !isGeneratingFolderChildren &&
                                    !isGeneratingNotes &&
                                    !isGeneratingBenchmarkDataset,
                            onClick = onGenerateFolderChildren,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .padding(horizontal = SETTINGS_SECTION_HORIZONTAL_PADDING)
                        .fillMaxWidth(),
                    shape = SmoothRoundedCornerShape(radius = SETTINGS_SECTION_CARD_RADIUS),
                    color = settingsSectionCardColor()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = generateBenchmarkDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        DebugActionButton(
                            text = generateBenchmarkButton,
                            enabled = !isGeneratingBenchmarkDataset &&
                                    !isGeneratingNotes &&
                                    !isGeneratingFolderChildren,
                            onClick = onGenerateBenchmarkDataset,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
