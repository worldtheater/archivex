package com.worldtheater.archive.feature.note_list.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import archivex.composeapp.generated.resources.Res
import archivex.composeapp.generated.resources.action_cancel
import archivex.composeapp.generated.resources.action_confirm
import archivex.composeapp.generated.resources.action_delete
import archivex.composeapp.generated.resources.action_lock
import archivex.composeapp.generated.resources.action_move
import archivex.composeapp.generated.resources.action_ok
import archivex.composeapp.generated.resources.action_unlock
import archivex.composeapp.generated.resources.back_desc
import archivex.composeapp.generated.resources.clear_desc
import archivex.composeapp.generated.resources.details_label_created_at
import archivex.composeapp.generated.resources.details_label_updated_at
import archivex.composeapp.generated.resources.dialog_create_folder_desc
import archivex.composeapp.generated.resources.dialog_create_folder_title
import archivex.composeapp.generated.resources.dialog_sensitive_lock_title_desc
import archivex.composeapp.generated.resources.dialog_sensitive_lock_title_title
import archivex.composeapp.generated.resources.folder_details_label_current
import archivex.composeapp.generated.resources.folder_details_label_descendants
import archivex.composeapp.generated.resources.folder_details_label_path
import archivex.composeapp.generated.resources.folder_details_label_protection
import archivex.composeapp.generated.resources.folder_details_value_direct_items_fmt
import archivex.composeapp.generated.resources.folder_details_value_total_descendants_fmt
import archivex.composeapp.generated.resources.folder_name_placeholder
import archivex.composeapp.generated.resources.menu_details
import archivex.composeapp.generated.resources.menu_strong_password_generator
import archivex.composeapp.generated.resources.menu_settings
import archivex.composeapp.generated.resources.msg_folder_name_too_long_fmt
import archivex.composeapp.generated.resources.msg_title_too_long_fmt
import archivex.composeapp.generated.resources.no_notes
import archivex.composeapp.generated.resources.note_details_label_char_count
import archivex.composeapp.generated.resources.note_details_label_protection
import archivex.composeapp.generated.resources.note_details_value_char_count_fmt
import archivex.composeapp.generated.resources.note_details_value_not_protected
import archivex.composeapp.generated.resources.note_details_value_protected
import archivex.composeapp.generated.resources.new_folder
import archivex.composeapp.generated.resources.pref_clear_clipboard_title
import archivex.composeapp.generated.resources.search_desc
import archivex.composeapp.generated.resources.search_placeholder
import archivex.composeapp.generated.resources.sensitive_note_title_fallback
import archivex.composeapp.generated.resources.sensitive_title_input_placeholder
import com.worldtheater.archive.domain.model.ITEM_TYPE_FOLDER
import com.worldtheater.archive.domain.model.NoteInfo
import com.worldtheater.archive.feature.note_list.components.rows.MinimalNoteRow
import com.worldtheater.archive.security.PasswordStrengthLevel
import com.worldtheater.archive.ui.shape.SmoothRoundedCornerShape
import com.worldtheater.archive.ui.theme.DefaultAppBarButtonSize
import com.worldtheater.archive.ui.theme.appOnSurfaceA05
import com.worldtheater.archive.ui.theme.appOnSurfaceA10
import com.worldtheater.archive.ui.theme.appOnSurfaceA30
import com.worldtheater.archive.ui.theme.appOnSurfaceA50
import com.worldtheater.archive.ui.theme.appOnSurfaceA80
import com.worldtheater.archive.ui.theme.appOnSurfaceA90
import com.worldtheater.archive.ui.theme.isAppInDarkTheme
import com.worldtheater.archive.ui.widget.AppDialog
import com.worldtheater.archive.ui.widget.AppDialogTitle
import com.worldtheater.archive.ui.widget.AutoResizingText
import com.worldtheater.archive.ui.widget.BlurredTopBarContainer
import com.worldtheater.archive.ui.widget.CapsuleContainer
import com.worldtheater.archive.ui.widget.CenteredAppTopBarContent
import com.worldtheater.archive.ui.widget.IosAlert
import com.worldtheater.archive.ui.widget.PopupMenu
import com.worldtheater.archive.ui.widget.PopupMenuHorizontalAlign
import com.worldtheater.archive.ui.widget.PopupMenuVerticalAlign
import com.worldtheater.archive.ui.widget.capsuleBgColor
import com.worldtheater.archive.ui.widget.capsuleContentColor
import com.worldtheater.archive.ui.widget.moveHintTextA90
import com.worldtheater.archive.ui.widget.offsetShadow
import com.worldtheater.archive.ui.widget.scrollbar
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun FolderListContent(
    viewModel: NoteListViewModel,
    noteList: List<NoteInfo>,
    noteById: Map<Int, NoteInfo>,
    sortedChildrenByParent: Map<String?, List<NoteInfo>>,
    currentFolderId: Int?,
    isSearchActive: Boolean,
    searchQuery: String,
    folderNavDirection: Int,
    searchListState: LazyListState,
    normalListStateForFolder: (Int?) -> LazyListState,
    contentTopPadding: Dp,
    isMoveMode: Boolean,
    movingItemId: Int?,
    openContextMenuNoteId: Int?,
    onEnterFolder: (folderId: Int, fromSearchResult: Boolean) -> Unit,
    onOpenNote: (Int) -> Unit,
    onDeleteClick: (NoteInfo) -> Unit,
    onDetailsClick: (NoteInfo) -> Unit,
    onMoveClick: (NoteInfo) -> Unit,
    onToggleSensitiveClick: (NoteInfo) -> Unit,
    onMenuVisibilityChange: (noteId: Int, show: Boolean) -> Unit,
    onLongPressFeedback: () -> Unit
) {
    val noNotesText = stringResource(Res.string.no_notes)
    val sensitiveNoteTitleFallbackText = stringResource(Res.string.sensitive_note_title_fallback)
    val detailsMenuText = stringResource(Res.string.menu_details)
    val deleteMenuText = stringResource(Res.string.action_delete)
    val moveMenuText = stringResource(Res.string.action_move)
    val lockMenuText = stringResource(Res.string.action_lock)
    val unlockMenuText = stringResource(Res.string.action_unlock)

    @Composable
    fun renderList(displayItems: List<NoteInfo>, listState: LazyListState) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (displayItems.isEmpty()) {
                Text(
                    text = noNotesText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Normal,
                        color = appOnSurfaceA10()
                    ),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = contentTopPadding,
                    bottom = 66.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .scrollbar(
                        state = listState,
                        horizontal = false,
                        thickness = 4.dp,
                        knobCornerRadius = 0.dp,
                        fixedKnobRatio = 0.1f,
                        knobColor = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    ),
                verticalArrangement = Arrangement.Top
            ) {
                itemsIndexed(
                    displayItems,
                    key = { _, info -> info._id },
                    contentType = { _, info -> info.itemType }
                ) { _, info ->
                    val canInteractInMoveMode = !isMoveMode ||
                        (info.itemType == ITEM_TYPE_FOLDER && info._id != movingItemId)
                    MinimalNoteRow(
                        note = info,
                        sensitiveNoteTitleFallback = sensitiveNoteTitleFallbackText,
                        detailsMenuText = detailsMenuText,
                        deleteMenuText = deleteMenuText,
                        moveMenuText = moveMenuText,
                        lockMenuText = lockMenuText,
                        unlockMenuText = unlockMenuText,
                        onLongPressFeedback = onLongPressFeedback,
                        enabled = canInteractInMoveMode,
                        onItemClick = {
                            if (isMoveMode && (info.itemType != ITEM_TYPE_FOLDER || info._id == movingItemId)) {
                                return@MinimalNoteRow
                            }
                            if (info.itemType == ITEM_TYPE_FOLDER) {
                                onEnterFolder(info._id, searchQuery.isNotBlank())
                                return@MinimalNoteRow
                            }
                            if (!isMoveMode) {
                                onOpenNote(info._id)
                            }
                        },
                        onDeleteClick = onDeleteClick,
                        onDetailsClick = onDetailsClick,
                        onMoveClick = if (isSearchActive) null else onMoveClick,
                        onToggleSensitiveClick = onToggleSensitiveClick,
                        showMenu = !isMoveMode && openContextMenuNoteId == info._id,
                        onShowMenuChange = { show ->
                            if (!isMoveMode) {
                                onMenuVisibilityChange(info._id, show)
                            }
                        }
                    )
                }
            }
        }
    }

    if (searchQuery.isNotBlank()) {
        renderList(
            viewModel.visibleItemsForFolder(
                noteList = noteList,
                noteById = noteById,
                sortedChildrenByParent = sortedChildrenByParent,
                folderId = currentFolderId,
                searchQuery = searchQuery
            ),
            searchListState
        )
        return
    }

    AnimatedContent(
        targetState = currentFolderId,
        transitionSpec = {
            val offsetSign = if (folderNavDirection >= 0) 1 else -1
            (slideInHorizontally(
                animationSpec = tween(260),
                initialOffsetX = { fullWidth -> fullWidth * offsetSign }
            ) + fadeIn(animationSpec = tween(260, delayMillis = 20)))
                .togetherWith(
                    slideOutHorizontally(
                        animationSpec = tween(260),
                        targetOffsetX = { fullWidth -> -fullWidth * offsetSign / 4 }
                    ) + fadeOut(animationSpec = tween(220))
                )
        },
        label = "FolderTransition"
    ) { targetFolderId ->
        val folderListState = normalListStateForFolder(targetFolderId)
        renderList(
            viewModel.visibleItemsForFolder(
                noteList = noteList,
                noteById = noteById,
                sortedChildrenByParent = sortedChildrenByParent,
                folderId = targetFolderId,
                searchQuery = searchQuery
            ),
            folderListState
        )
    }
}

@Composable
fun BoxScope.NoteListTopBarLayer(
    isSearchActive: Boolean,
    modifier: Modifier = Modifier,
    normalContent: @Composable BoxScope.() -> Unit,
    searchContent: @Composable BoxScope.() -> Unit
) {
    BlurredTopBarContainer(
        modifier = modifier,
        extraContentHeight = 0.dp
    ) {
        val transition = updateTransition(targetState = isSearchActive, label = "TopBarMode")

        val searchAlpha = transition.animateFloat(
            transitionSpec = { tween(durationMillis = 220) },
            label = "SearchAlpha"
        ) { active -> if (active) 1f else 0f }

        val normalAlpha = transition.animateFloat(
            transitionSpec = { tween(durationMillis = 220) },
            label = "NormalAlpha"
        ) { active -> if (active) 0f else 1f }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { clip = false }
        ) {
            if (normalAlpha.value > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = normalAlpha.value
                            clip = false
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                        }
                ) { normalContent() }
            }

            if (searchAlpha.value > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = searchAlpha.value
                            clip = false
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                        }
                ) { searchContent() }
            }
        }
    }
}

@Composable
fun BoxScope.NormalNoteListTopBarContent(
    totalNotesText: String,
    currentFolderName: String?,
    centerContentVisible: Boolean,
    isMoveMode: Boolean,
    hasParentFolder: Boolean,
    onNavigationClick: () -> Unit,
    onCreateFolderClick: () -> Unit,
    onStrongPasswordGeneratorClick: () -> Unit,
    onClearClipboardClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExitMoveMode: () -> Unit
) {
    val backDescription = stringResource(Res.string.back_desc)
    val searchDescription = stringResource(Res.string.search_desc)
    val cancelDescription = stringResource(Res.string.action_cancel)
    val settingsDescription = stringResource(Res.string.menu_settings)
    val newFolder = stringResource(Res.string.new_folder)
    val strongPasswordGenerator = stringResource(Res.string.menu_strong_password_generator)
    val clearClipboard = stringResource(Res.string.pref_clear_clipboard_title)
    val settings = stringResource(Res.string.menu_settings)
    var showTopMenu by remember { mutableStateOf(false) }
    var topMenuAnchor by remember { mutableStateOf(IntOffset.Zero) }
    val moveModeBgColor = capsuleBgColor()
    val moveModeContentColor = capsuleContentColor()
    val navigationContent: (@Composable () -> Unit)? = if (isMoveMode && !hasParentFolder) {
        null
    } else {
        {
            IconButton(
                onClick = onNavigationClick,
                modifier = Modifier.size(DefaultAppBarButtonSize)
            ) {
                Icon(
                    if (hasParentFolder) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Search,
                    if (hasParentFolder) backDescription else searchDescription
                )
            }
        }
    }
    val actionContent: (@Composable () -> Unit)? = if (isMoveMode) {
        {
            IconButton(
                onClick = onExitMoveMode,
                modifier = Modifier.size(DefaultAppBarButtonSize)
            ) {
                Icon(Icons.Filled.Close, cancelDescription)
            }
        }
    } else {
        {
            Box {
                IconButton(
                    onClick = { showTopMenu = true },
                    modifier = Modifier
                        .size(DefaultAppBarButtonSize)
                        .onGloballyPositioned { coordinates ->
                            topMenuAnchor = IntOffset(coordinates.size.width, 0)
                        }
                ) {
                    Icon(Icons.Filled.MoreHoriz, settingsDescription)
                }
                val topMenuItems = linkedMapOf<String, () -> Unit>().apply {
                    put(newFolder, onCreateFolderClick)
                    put(strongPasswordGenerator, onStrongPasswordGeneratorClick)
                    put(clearClipboard, onClearClipboardClick)
                    put(settings, onSettingsClick)
                }
                PopupMenu(
                    menuItems = topMenuItems,
                    showMenu = showTopMenu,
                    onDismiss = { showTopMenu = false },
                    anchorPosition = topMenuAnchor,
                    minWidth = 84.dp,
                    maxWidth = 168.dp,
                    horizontalAlign = PopupMenuHorizontalAlign.END,
                    verticalAlign = PopupMenuVerticalAlign.TOP,
                    anchorVerticalSpacing = 0.dp
                )
            }
        }
    }

    CenteredAppTopBarContent(
        centerContent = {
            AutoResizingText(
                text = currentFolderName ?: totalNotesText,
                style = MaterialTheme.typography.titleMedium
            )
        },
        centerContentVisible = centerContentVisible,
        elementBackgroundColor = moveModeBgColor,
        elementContentColor = moveModeContentColor,
        navigationIcon = navigationContent,
        actions = actionContent
    )
}

@Composable
fun BoxScope.NoteListSearchTopAppBarContent(
    searchQuery: String,
    onBack: () -> Unit,
    onSearchChange: (String) -> Unit,
    onClear: () -> Unit
) {
    val backDescription = stringResource(Res.string.back_desc)
    val clearDescription = stringResource(Res.string.clear_desc)
    val searchPlaceholder = stringResource(Res.string.search_placeholder)
    CapsuleContainer(
        modifier = Modifier.align(Alignment.CenterStart),
        isCircle = true
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(DefaultAppBarButtonSize)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, backDescription)
        }
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    CapsuleContainer(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 60.dp)
            .fillMaxWidth(),
        isCircle = false
    ) {
        BasicTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            singleLine = true,
            textStyle = TextStyle(
                color = capsuleContentColor(),
                fontSize = 16.sp
            ),
            cursorBrush = SolidColor(capsuleContentColor()),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        if (searchQuery.isEmpty()) {
                            Text(
                                searchPlaceholder,
                                style = TextStyle(
                                    color = appOnSurfaceA50(),
                                    fontSize = 16.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            }
        )
    }

    if (searchQuery.isNotEmpty()) {
        CapsuleContainer(
            modifier = Modifier.align(Alignment.CenterEnd),
            isCircle = true
        ) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(DefaultAppBarButtonSize)
            ) {
                Icon(Icons.Filled.Close, clearDescription)
            }
        }
    }
}

@Composable
fun CreateFolderDialog(
    maxLength: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val title = stringResource(Res.string.dialog_create_folder_title)
    val description = stringResource(Res.string.dialog_create_folder_desc)
    val confirmText = stringResource(Res.string.action_ok)
    val cancelText = stringResource(Res.string.action_cancel)
    val placeholder = stringResource(Res.string.folder_name_placeholder)
    val tooLongError = stringResource(Res.string.msg_folder_name_too_long_fmt, maxLength)
    NameInputDialog(
        key = "create_folder",
        maxLength = maxLength,
        title = title,
        description = description,
        confirmText = confirmText,
        cancelText = cancelText,
        placeholder = placeholder,
        tooLongError = tooLongError,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun RequireTitleForLockDialog(
    key: String,
    maxLength: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val title = stringResource(Res.string.dialog_sensitive_lock_title_title)
    val description = stringResource(Res.string.dialog_sensitive_lock_title_desc)
    val confirmText = stringResource(Res.string.action_ok)
    val cancelText = stringResource(Res.string.action_cancel)
    val placeholder = stringResource(Res.string.sensitive_title_input_placeholder)
    val tooLongError = stringResource(Res.string.msg_title_too_long_fmt, maxLength)
    NameInputDialog(
        key = key,
        maxLength = maxLength,
        title = title,
        description = description,
        confirmText = confirmText,
        cancelText = cancelText,
        placeholder = placeholder,
        tooLongError = tooLongError,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun DeleteConfirmDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val confirmText = stringResource(Res.string.action_confirm)
    val cancelText = stringResource(Res.string.action_cancel)
    AppDialog(onDismissRequest = onDismiss) { triggerClose ->
        IosAlert(
            title = title,
            description = description,
            primaryText = confirmText,
            onPrimaryClick = { triggerClose { onConfirm() } },
            secondaryText = cancelText,
            onSecondaryClick = { triggerClose { onDismiss() } }
        )
    }
}

@Composable
private fun NameInputDialog(
    key: String,
    maxLength: Int,
    title: String,
    description: String,
    confirmText: String,
    cancelText: String,
    placeholder: String,
    tooLongError: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var input by rememberSaveable(key) { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    var showTooLongError by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    AppDialog(onDismissRequest = onDismiss) { triggerClose ->
        LaunchedEffect(key) {
            focusRequester.requestFocus()
        }

        IosAlert(
            title = title,
            description = description,
            primaryText = confirmText,
            primaryButtonEnabled = input.isNotBlank() && !showTooLongError,
            onPrimaryClick = {
                val trimmed = input.trim()
                if (trimmed.isBlank()) return@IosAlert
                triggerClose { onConfirm(trimmed) }
            },
            secondaryText = cancelText,
            onSecondaryClick = { triggerClose { onDismiss() } }
        ) {
            NameInputField(
                value = input,
                onValueChange = {
                    if (it.length <= maxLength) {
                        input = it
                        if (showTooLongError && it.length < maxLength) {
                            showTooLongError = false
                        }
                    } else {
                        showTooLongError = true
                    }
                },
                placeholder = placeholder,
                isFocused = isFocused,
                onFocusedChange = { isFocused = it },
                focusRequester = focusRequester
            )

            if (showTooLongError) {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = tooLongError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun NameInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isFocused: Boolean,
    onFocusedChange: (Boolean) -> Unit,
    focusRequester: FocusRequester
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
    val colors = nameInputColors(MaterialTheme.colorScheme, isFocused)

    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = textStyle.copy(color = appOnSurfaceA30())
            )
        },
        textStyle = textStyle,
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusedChange(it.isFocused) },
        colors = colors,
        shape = SmoothRoundedCornerShape(18.dp)
    )
}

@Composable
private fun nameInputColors(
    colorScheme: ColorScheme,
    isFocused: Boolean
) = TextFieldDefaults.colors(
    focusedContainerColor = if (isFocused) appOnSurfaceA05() else Color.Transparent,
    unfocusedContainerColor = if (isFocused) appOnSurfaceA05() else Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    focusedTextColor = colorScheme.onSurface,
    unfocusedTextColor = colorScheme.onSurface
)

@Composable
fun MoveModeFloatingHint(
    title: String,
    hint: String,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val containerColor = appOnSurfaceA90()
    val textColor = if (isDark) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface
    Surface(
        modifier = modifier.offsetShadow(),
        shape = SmoothRoundedCornerShape(18.dp),
        color = containerColor,
        contentColor = textColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = moveHintTextA90()
            )
        }
    }
}

@Composable
fun ItemDetailsDialog(
    title: String,
    details: List<Pair<String, String>>,
    onDismiss: () -> Unit
) {
    AppDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = SmoothRoundedCornerShape(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(start = 24.dp, top = 18.dp, end = 24.dp, bottom = 24.dp)
            ) {
                AppDialogTitle(
                    text = title,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.size(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    details.forEach { (label, value) ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = appOnSurfaceA50()
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodySmall,
                                color = appOnSurfaceA80()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun buildItemDetailsRows(
    item: NoteInfo,
    noteList: List<NoteInfo>,
    itemPath: String,
    actualCharCount: Int,
    descendantCount: Int,
    createdAtText: String,
    updatedAtText: String
): List<Pair<String, String>> {
    val folderPathLabel = stringResource(Res.string.folder_details_label_path)
    val folderCurrentLabel = stringResource(Res.string.folder_details_label_current)
    val folderDescendantsLabel = stringResource(Res.string.folder_details_label_descendants)
    val folderProtectionLabel = stringResource(Res.string.folder_details_label_protection)
    val protectionLabel = stringResource(Res.string.note_details_label_protection)
    val protectionProtectedValue = stringResource(Res.string.note_details_value_protected)
    val protectionUnprotectedValue = stringResource(Res.string.note_details_value_not_protected)
    val charCountLabel = stringResource(Res.string.note_details_label_char_count)
    val createdAtLabel = stringResource(Res.string.details_label_created_at)
    val updatedAtLabel = stringResource(Res.string.details_label_updated_at)

    return if (item.itemType == ITEM_TYPE_FOLDER) {
        val directChildren = noteList.filter { it.parentNodeId == item.nodeId }
        val directFolderCount = directChildren.count { it.itemType == ITEM_TYPE_FOLDER }
        val directNoteCount = directChildren.count { it.itemType != ITEM_TYPE_FOLDER }
        val directChildrenValue = if (item.isSensitive) {
            "*"
        } else {
            stringResource(
                Res.string.folder_details_value_direct_items_fmt,
                directChildren.size,
                directFolderCount,
                directNoteCount
            )
        }
        val descendantsValue = if (item.isSensitive) {
            "*"
        } else {
            stringResource(Res.string.folder_details_value_total_descendants_fmt, descendantCount)
        }
        listOf(
            folderPathLabel to itemPath,
            folderCurrentLabel to directChildrenValue,
            folderDescendantsLabel to descendantsValue,
            folderProtectionLabel to if (item.isSensitive) {
                protectionProtectedValue
            } else {
                protectionUnprotectedValue
            },
            createdAtLabel to createdAtText,
            updatedAtLabel to updatedAtText
        )
    } else {
        buildList {
            add(folderPathLabel to itemPath)
            add(
                protectionLabel to if (item.isSensitive) {
                    protectionProtectedValue
                } else {
                    protectionUnprotectedValue
                }
            )
            if (!item.isSensitive) {
                add(charCountLabel to stringResource(Res.string.note_details_value_char_count_fmt, actualCharCount))
            }
            add(createdAtLabel to createdAtText)
            add(updatedAtLabel to updatedAtText)
        }
    }
}
