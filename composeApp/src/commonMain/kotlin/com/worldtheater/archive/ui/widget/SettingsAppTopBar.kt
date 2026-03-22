package com.worldtheater.archive.ui.widget

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import archivex.composeapp.generated.resources.Res
import archivex.composeapp.generated.resources.back_desc
import com.worldtheater.archive.ui.theme.DefaultAppBarButtonSize
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsAppTopBar(
    title: String,
    onBack: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val centerContentVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }

    CenteredAppTopBar(
        title = {},
        centerContent = {
            AutoResizingText(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
        },
        centerContentVisible = centerContentVisible,
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier.Companion.size(DefaultAppBarButtonSize)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back_desc))
            }
        },
        modifier = modifier
    )
}
