package com.worldtheater.archive.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldtheater.archive.util.MarkdownUtils

@Composable
fun MarkdownShortcutBar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    leadingActions: (@Composable RowScope.() -> Unit)? = null,
    extraActions: (@Composable RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (leadingActions != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                content = leadingActions
            )
            Spacer(Modifier.width(8.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.weight(1f)
        ) {
            ShortcutButton(
                text = "#",
                onClick = { onValueChange(MarkdownUtils.insertHeader(value)) })
            ShortcutButton(text = "-", onClick = { onValueChange(MarkdownUtils.insertList(value)) })
            ShortcutButton(
                text = "B",
                bold = true,
                onClick = { onValueChange(MarkdownUtils.toggleBold(value)) })
            ShortcutButton(
                text = "I",
                italic = true,
                onClick = { onValueChange(MarkdownUtils.toggleItalic(value)) })
            ShortcutButton(
                text = "`",
                onClick = { onValueChange(MarkdownUtils.toggleCode(value)) })
            ShortcutButton(
                text = "”",
                onClick = { onValueChange(MarkdownUtils.insertQuote(value)) })
        }

        if (extraActions != null) {
            Spacer(Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                content = extraActions
            )
        }
    }
}

@Composable
private fun ShortcutButton(
    text: String,
    bold: Boolean = false,
    italic: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .width(48.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
