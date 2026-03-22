package com.worldtheater.archive.feature.note_list.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldtheater.archive.ui.theme.appOnSurfaceA40
import com.worldtheater.archive.ui.theme.appOnSurfaceA60
import com.worldtheater.archive.ui.widget.MarkdownShortcutBar
import com.worldtheater.archive.ui.widget.MarkdownUndoRedoLeadingActions
import com.worldtheater.archive.ui.widget.scrollbar
import kotlinx.coroutines.flow.collectLatest

data class NoteEditContentStrings(
    val placeholder: String,
    val undoDescription: String,
    val redoDescription: String
)

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun NoteEditContent(
    bodyText: String,
    bodyTextFieldValue: TextFieldValue,
    onBodyChange: (TextFieldValue) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    scrollState: ScrollState,
    focusRequester: FocusRequester,
    strings: NoteEditContentStrings,
    modifier: Modifier = Modifier,
    contentTopPadding: androidx.compose.ui.unit.Dp
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .scrollbar(
                state = scrollState,
                horizontal = false,
                alignEnd = true,
                knobColor = appOnSurfaceA60(),
                trackColor = Color.Transparent,
                padding = 2.dp
            )
    ) {
        val maxHeight = this.maxHeight
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
            var maxObservedHeightPx by remember { mutableFloatStateOf(0f) }
            val density = LocalDensity.current
            val imeInsets = WindowInsets.ime
            val currentBodyInput by rememberUpdatedState(bodyTextFieldValue)
            val imeVisible = imeInsets.getBottom(density) > 0

            LaunchedEffect(maxHeight, density) {
                val currentHeightPx = with(density) { maxHeight.toPx() }
                if (currentHeightPx > maxObservedHeightPx) {
                    maxObservedHeightPx = currentHeightPx
                }
            }

            LaunchedEffect(Unit) {
                snapshotFlow {
                    Triple(
                        imeInsets.getBottom(density),
                        currentBodyInput.selection,
                        textLayoutResult
                    )
                }.collectLatest { (imeBottom, selection, layout) ->
                    if (imeBottom > 0 && layout != null && selection.start <= layout.layoutInput.text.length) {
                        val cursorRect = layout.getCursorRect(selection.start)
                        val cursorBottomInTextField = cursorRect.bottom

                        val spacerTopPx = with(density) { (contentTopPadding + 16.dp).toPx() }
                        val cursorBottomInContainer = spacerTopPx + cursorBottomInTextField

                        val currentScroll = scrollState.value
                        val cursorBottomOnScreen = cursorBottomInContainer - currentScroll

                        val toolbarHeightPx = with(density) { 60.dp.toPx() }
                        val currentBoxHeightPx = with(density) { maxHeight.toPx() }
                        val fullScreenHeightPx = if (maxObservedHeightPx > 0f) {
                            maxObservedHeightPx
                        } else {
                            currentBoxHeightPx
                        }
                        val heightDiff = fullScreenHeightPx - currentBoxHeightPx
                        val isAlreadyResized = heightDiff > with(density) { 100.dp.toPx() }
                        val effectiveImeSubtraction =
                            if (isAlreadyResized) 0f else imeBottom.toFloat()

                        val safeVisibleBottom =
                            currentBoxHeightPx - effectiveImeSubtraction - toolbarHeightPx

                        if (cursorBottomOnScreen > safeVisibleBottom) {
                            val scrollDelta =
                                cursorBottomOnScreen - safeVisibleBottom + with(density) { 20.dp.toPx() }
                            scrollState.animateScrollTo((currentScroll + scrollDelta).toInt())
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(Modifier.height(contentTopPadding + 12.dp))

                    Box {
                        if (bodyText.isEmpty()) {
                            Text(
                                text = strings.placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = appOnSurfaceA40()
                            )
                        }
                        BasicTextField(
                            value = bodyTextFieldValue,
                            onValueChange = onBodyChange,
                            onTextLayout = { textLayoutResult = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .heightIn(min = 120.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 24.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                        )
                    }

                    Spacer(Modifier.height(60.dp))
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.ime))
                }
            }

            AnimatedVisibility(
                visible = imeVisible,
                enter = fadeIn(animationSpec = tween(100)),
                exit = fadeOut(animationSpec = tween(60)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
            ) {
                MarkdownShortcutBar(
                    value = bodyTextFieldValue,
                    onValueChange = onBodyChange,
                    leadingActions = {
                        MarkdownUndoRedoLeadingActions(
                            canUndo = canUndo,
                            canRedo = canRedo,
                            undoDescription = strings.undoDescription,
                            redoDescription = strings.redoDescription,
                            onUndo = onUndo,
                            onRedo = onRedo
                        )
                    }
                )
            }
        }
    }
}
