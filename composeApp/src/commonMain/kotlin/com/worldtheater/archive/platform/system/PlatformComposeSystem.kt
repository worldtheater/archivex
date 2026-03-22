package com.worldtheater.archive.platform.system

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable

@Composable
expect fun AppBackHandler(enabled: Boolean = true, onBack: () -> Unit)

@OptIn(ExperimentalFoundationApi::class)
fun disableNewContextMenuCompat() {
    ComposeFoundationFlags.isNewContextMenuEnabled = false
}
