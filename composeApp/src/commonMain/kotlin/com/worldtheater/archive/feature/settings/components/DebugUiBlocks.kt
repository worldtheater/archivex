package com.worldtheater.archive.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldtheater.archive.ui.shape.SmoothCapsuleShape
import com.worldtheater.archive.ui.shape.SmoothRoundedCornerShape
import com.worldtheater.archive.ui.theme.appOnSurfaceA05
import com.worldtheater.archive.ui.theme.appOnSurfaceA10
import com.worldtheater.archive.ui.theme.appOnSurfaceA30

@Composable
fun DebugActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(SmoothCapsuleShape())
            .background(appOnSurfaceA10())
            .then(
                if (enabled) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .alpha(if (enabled) 1f else 0.55f),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp
        )
    }
}

@Composable
fun DebugNumberField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
    TextField(
        value = value,
        onValueChange = { input ->
            if (input.all { it.isDigit() }) {
                onValueChange(input)
            }
        },
        placeholder = {
            Text(
                text = "0",
                style = textStyle.copy(color = appOnSurfaceA30())
            )
        },
        singleLine = true,
        textStyle = textStyle,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = appOnSurfaceA05(),
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = SmoothRoundedCornerShape(16.dp)
    )
}
