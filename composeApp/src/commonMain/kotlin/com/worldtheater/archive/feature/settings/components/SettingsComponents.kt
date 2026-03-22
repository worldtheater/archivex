package com.worldtheater.archive.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldtheater.archive.platform.system.currentTimeMillis
import com.worldtheater.archive.ui.theme.appOnSurfaceA08
import com.worldtheater.archive.ui.theme.appOnSurfaceA30
import com.worldtheater.archive.ui.theme.appOnSurfaceA40
import com.worldtheater.archive.ui.theme.appOnSurfaceA45

@Composable
fun debouncedClick(onClick: () -> Unit): () -> Unit {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    return {
        val currentTime = currentTimeMillis()
        if (currentTime - lastClickTime > 500) {
            lastClickTime = currentTime
            onClick()
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    debounceClick: Boolean = true,
    onClick: (() -> Unit)? = null,
    action: (@Composable RowScope.() -> Unit)? = null
) {
    val safeOnClick = if (enabled && onClick != null) {
        if (debounceClick) debouncedClick(onClick) else onClick
    } else {
        null
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val titleColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        appOnSurfaceA40()
    }
    val subtitleColor = if (enabled) {
        Color.Gray
    } else {
        appOnSurfaceA30()
    }
    val canNavigate = enabled && onClick != null && action == null
    val pressedBg = if (isPressed) appOnSurfaceA08() else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(pressedBg)
            .then(
                if (safeOnClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = safeOnClick,
                        onClickLabel = title
                    )
                } else {
                    Modifier
                }
            )
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 17.sp
                    ),
                    color = subtitleColor
                )
            }
        }
        if (action != null) {
            action()
        } else if (canNavigate) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = appOnSurfaceA45()
            )
        }
    }
}
