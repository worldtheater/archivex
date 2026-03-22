package com.worldtheater.archive.feature.note_list.components.rows

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import archivex.composeapp.generated.resources.Res
import archivex.composeapp.generated.resources.folder_limited_24px
import com.worldtheater.archive.domain.model.ITEM_TYPE_FOLDER
import com.worldtheater.archive.domain.model.NoteInfo
import com.worldtheater.archive.ui.theme.*
import com.worldtheater.archive.ui.widget.PopupMenu
import com.worldtheater.archive.util.StringUtils
import com.worldtheater.archive.util.log.L
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

private const val TAG = "MinimalNoteRow"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MinimalNoteRow(
    note: NoteInfo,
    sensitiveNoteTitleFallback: String,
    detailsMenuText: String,
    deleteMenuText: String,
    moveMenuText: String,
    lockMenuText: String,
    unlockMenuText: String,
    onLongPressFeedback: () -> Unit,
    enabled: Boolean = true,
    onItemClick: (NoteInfo) -> Unit,
    onDeleteClick: (NoteInfo) -> Unit,
    onDetailsClick: (NoteInfo) -> Unit,
    onMoveClick: ((NoteInfo) -> Unit)? = null,
    onToggleSensitiveClick: ((NoteInfo) -> Unit)? = null,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
) {
    var pressPosition by remember { mutableStateOf(IntOffset.Zero) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOverlayColor = if (isAppInDarkTheme()) appOnSurfaceA10() else appOnSurfaceA05()

    // Subtle highlight on press/menu
    // Use Transparent for idle state to ensure smooth alpha transition without color shifting artifacts
    val targetColor =
        if (enabled && (showMenu || isPressed)) {
            pressOverlayColor
        } else Color.Transparent

    val animatedOverlayColor by animateColorAsState(
        targetValue = targetColor,
        label = "NoteRowOverlay",
        animationSpec = if (showMenu || isPressed) tween(100) else tween(200)
    )
    val folderTitle = remember(note.title, note._id) { note.title.ifBlank { "Folder" } }
    val sensitiveTitle = remember(note.title) {
        note.title.ifBlank { "" }
    }
    val titledPreview = remember(note.body) { previewText(note.body, 150) }
    val plainBodyUseChips = remember(note.body) { shouldUseChips(note.body) }
    val plainBodyPreview = remember(note.body) {
        previewText(note.body, maxLen = 180).replace(Regex("\\n{3,}"), "\n\n")
    }
    val plainBodyTags = remember(note.body, plainBodyUseChips) {
        if (plainBodyUseChips) StringUtils.smartSplit(note.body) else emptyList()
    }

    // Neutral Color Logic removed as per request.
    // Chips are now unified. Title dot is also removed or made generic if needed.

    Row {
        // Removed Canvas (Vertical Bar)

        Column(
            modifier = Modifier
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            var event = awaitPointerEvent(PointerEventPass.Initial)
                            // Wait for the first down event
                            while (event.changes.all { !it.pressed }) {
                                event = awaitPointerEvent(PointerEventPass.Initial)
                            }

                            val change = event.changes.find { it.pressed }
                            if (change != null) {
                                pressPosition = IntOffset(
                                    x = change.position.x.roundToInt(),
                                    y = change.position.y.roundToInt()
                                )
                                if (event.buttons.isSecondaryPressed) {
                                    onShowMenuChange(true)
                                }
                            }

                            // Wait for all pointers to be up
                            while (event.changes.any { it.pressed }) {
                                event = awaitPointerEvent(PointerEventPass.Initial)
                            }
                        }
                    }
                }
                .fillMaxWidth()
                .wrapContentHeight()
                .background(MaterialTheme.colorScheme.background) // Base background
                .background(animatedOverlayColor) // Interaction overlay
                .combinedClickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onLongClick = {
                        L.v(TAG, "InfoList: long click! ${note.body}")
                        onLongPressFeedback()
                        onShowMenuChange(true)
                    }
                ) {
                    onItemClick(note)
                }
                .padding(0.dp) // Remove outer padding so highlight goes to edge
        ) {
            // Content Container with Padding
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, start = 12.dp, end = 12.dp, bottom = 8.dp)
                    .then(if (enabled) Modifier else Modifier.alpha(0.45f))
            ) {
                // Header: Title or Date/Indicator?
                if (note.itemType == ITEM_TYPE_FOLDER) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (note.isSensitive) {
                            Icon(
                                painter = painterResource(Res.drawable.folder_limited_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = folderTitle,
                            style = TextStyle(
                                fontWeight = FontWeight.Normal,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier.padding(start = 6.dp),
                            maxLines = 1
                        )
                    }
                } else if (note.isSensitive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sensitiveTitle.ifBlank {
                                sensitiveNoteTitleFallback
                            },
                            style = TextStyle(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            maxLines = 2
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(
                            modifier = Modifier
                                .width(152.dp)
                                .height(10.dp)
                                .background(
                                    color = appOnSurfaceA50(),
                                )
                        )
                        Box(
                            modifier = Modifier
                                .width(128.dp)
                                .height(10.dp)
                                .background(
                                    color = appOnSurfaceA40(),
                                )
                        )
                    }
                } else if (note.title.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = note.title.replace("\n", " "),
                            style = TextStyle(
                                fontWeight = FontWeight.SemiBold, // Slightly less bold than before
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            maxLines = 2
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = titledPreview,
                        style = TextStyle(
                            fontSize = 15.sp,
                            color = appOnSurfaceA70(), // Muted body text
                            lineHeight = 20.sp,
                            fontStyle = FontStyle.Italic
                        ),
                        maxLines = 2, // Show a bit more text
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    // No title logic
                    // No title logic
                    val useChips = plainBodyUseChips

                    if (!useChips) {
                        // Long / sentence-like -> render as preview text (弱化一点，避免和 chip 抢语义位)
                        Text(
                            text = plainBodyPreview,
                            style = TextStyle(
                                fontSize = 15.sp,
                                lineHeight = 21.sp,
                                color = appOnSurfaceA70(),
                                fontWeight = FontWeight.Normal,
                                fontStyle = FontStyle.Italic
                            ),
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                    } else {
                        // Chip mode
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            val allTags = plainBodyTags
                            val maxTags = 24
                            val displayTags = allTags.take(maxTags)

                            displayTags.forEach { tag ->
                                MinimalNoteItem(tag)
                            }
                            if (allTags.size > maxTags) {
                                Text(
                                    text = "…",
                                    color = appOnSurfaceA55(),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Divider at the bottom, inset horizontally to match content
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = appOnSurfaceA10()
            )
        }

        val contextMenu = linkedMapOf(
            detailsMenuText to {
                onShowMenuChange(false)
                onDetailsClick(note)
            },
            deleteMenuText to {
                onShowMenuChange(false)
                onDeleteClick(note)
            }
        )
        if (onMoveClick != null) {
            contextMenu[moveMenuText] = {
                onShowMenuChange(false)
                onMoveClick(note)
            }
        }
        if (onToggleSensitiveClick != null) {
            contextMenu[if (note.isSensitive) unlockMenuText else lockMenuText] = {
                onShowMenuChange(false)
                onToggleSensitiveClick(note)
            }
        }
        PopupMenu(
            menuItems = contextMenu,
            showMenu = showMenu,
            anchorPosition = pressPosition,
            onDismiss = { onShowMenuChange(false) })
    }
}

private fun previewText(raw: String, maxLen: Int): String {
    val t = raw
        .replace(Regex("\\s+"), " ")
        .trim()
    return if (t.length > maxLen) t.take(maxLen) + "…" else t
}

/**
 * 更像“短语集合”才走 chip：
 * - 总长度不大
 * - 分割出来的 tag 数量 > 1
 * - 平均 tag 长度不太长（否则其实是句子）
 */
private fun shouldUseChips(body: String): Boolean {
    val normalized = body.trim()
    if (normalized.isEmpty()) return false

    val allTags = StringUtils.smartSplit(normalized)
//    if (allTags.size <= 1) return false

    val totalLen = normalized.length
    val sample = allTags.take(20)
    val avgLen = if (sample.isEmpty()) 0.0 else sample.sumOf { it.length }.toDouble() / sample.size

    return totalLen <= 220 && avgLen <= 50.0
}

