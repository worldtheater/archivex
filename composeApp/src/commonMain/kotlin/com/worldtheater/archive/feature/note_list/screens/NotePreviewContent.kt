package com.worldtheater.archive.feature.note_list.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldtheater.archive.feature.note_list.screens.markdown_preview.PlatformMarkdownPreviewContent
import com.worldtheater.archive.ui.theme.appOnSurfaceA60
import com.worldtheater.archive.ui.widget.scrollbar
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun NotePreviewContent(
    title: String,
    metadataText: String?,
    scrollState: ScrollState,
    contentTopPadding: Dp,
    showMarkdown: Boolean,
    isLongNote: Boolean,
    markdown: String,
    onMermaidImageClick: ((ImageBitmap, Int, Int) -> Unit)? = null,
    onAtTopChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value == 0 }
            .distinctUntilChanged()
            .collect { atTop ->
                onAtTopChanged?.invoke(atTop)
            }
    }

    Box(
        modifier = modifier
            .scrollbar(
                state = scrollState,
                horizontal = false,
                alignEnd = true,
                knobColor = appOnSurfaceA60(),
                trackColor = Color.Transparent,
                padding = 2.dp
            )
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 12.dp, end = 9.dp)
        ) {
            PreviewContentBody(
                title = title,
                metadataText = metadataText,
                contentTopPadding = contentTopPadding,
                showMarkdown = showMarkdown,
                isLongNote = isLongNote,
                markdown = markdown,
                onMermaidImageClick = onMermaidImageClick,
            )
        }
    }
}

@Composable
private fun PreviewContentBody(
    title: String,
    metadataText: String?,
    contentTopPadding: Dp,
    showMarkdown: Boolean,
    isLongNote: Boolean,
    markdown: String,
    onMermaidImageClick: ((ImageBitmap, Int, Int) -> Unit)?,
) {
    Column {
        Spacer(Modifier.height(contentTopPadding))

        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (!metadataText.isNullOrBlank()) {
            Text(
                text = metadataText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (showMarkdown) {
            PlatformMarkdownPreviewContent(
                markdown = markdown,
                onMermaidImageClick = onMermaidImageClick,
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (isLongNote) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}
