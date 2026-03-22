package com.worldtheater.archive.ui.widget

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.MarkdownPadding
import com.mikepenz.markdown.model.markdownPadding

private val PREVIEW_MARKDOWN_BLOCK_SPACING = 5.dp

@Composable
fun previewMarkdownPadding(): MarkdownPadding = markdownPadding(
    block = PREVIEW_MARKDOWN_BLOCK_SPACING
)

@Composable
fun previewMarkdownTypography() = markdownTypography(
    h1 = MaterialTheme.typography.headlineLarge,
    h2 = MaterialTheme.typography.headlineMedium,
    h3 = MaterialTheme.typography.headlineSmall,
    h4 = MaterialTheme.typography.titleLarge,
    h5 = MaterialTheme.typography.titleMedium,
    h6 = MaterialTheme.typography.titleSmall,
)
