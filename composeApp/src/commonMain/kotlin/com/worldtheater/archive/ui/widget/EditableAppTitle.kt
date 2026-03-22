package com.worldtheater.archive.ui.widget

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.worldtheater.archive.ui.theme.appOnSurfaceA50

const val NOTE_TITLE_MAX_LENGTH = 50

@Composable
fun EditableAppTitle(
    text: String,
    onTextChanged: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    placeCursorAtPlaceholderStartWhenEmptyFocused: Boolean = false
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        var isFocused by remember { mutableStateOf(false) }

        if (text.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.titleMedium,
                color = if (isFocused) appOnSurfaceA50() else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        val initialFontSize = MaterialTheme.typography.titleMedium.fontSize
        val smallFontSize = initialFontSize * 0.8f
        val textMeasurer = rememberTextMeasurer()
        val style = MaterialTheme.typography.titleMedium

        val useSmallFont = remember(text, maxWidth) {
            if (text.isEmpty()) {
                false
            } else {
                val measured = textMeasurer.measure(
                    text = text,
                    style = style.copy(fontSize = initialFontSize),
                    constraints = Constraints(maxWidth = constraints.maxWidth)
                )
                measured.lineCount > 1
            }
        }

        val titleFontSize = if (useSmallFont) smallFontSize else initialFontSize
        val density = LocalDensity.current
        val placeholderWidth = remember(placeholder, titleFontSize, maxWidth) {
            if (placeholder.isEmpty()) {
                0.dp
            } else {
                val measured = textMeasurer.measure(
                    text = placeholder,
                    style = style.copy(fontSize = titleFontSize),
                    constraints = Constraints(maxWidth = constraints.maxWidth)
                )
                with(density) { measured.size.width.toDp() }
            }
        }
        val isEmptyAndFocused =
            placeCursorAtPlaceholderStartWhenEmptyFocused && text.isEmpty() && isFocused

        BasicTextField(
            value = text,
            onValueChange = {
                if (it.length <= NOTE_TITLE_MAX_LENGTH) {
                    onTextChanged(it)
                }
            },
            modifier = Modifier
                .onFocusChanged { isFocused = it.isFocused }
                .composed {
                    if (isEmptyAndFocused && placeholderWidth > 0.dp) {
                        Modifier.width(placeholderWidth)
                    } else {
                        Modifier
                    }
                },
            textStyle = MaterialTheme.typography.titleMedium.copy(
                color = if (isFocused) appOnSurfaceA50() else MaterialTheme.colorScheme.onSurface,
                textAlign = if (isEmptyAndFocused) TextAlign.Start else TextAlign.Center,
                fontSize = titleFontSize
            ),
            maxLines = 2,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        )
    }
}
