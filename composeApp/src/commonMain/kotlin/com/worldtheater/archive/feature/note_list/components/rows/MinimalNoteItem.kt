package com.worldtheater.archive.feature.note_list.components.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldtheater.archive.ui.theme.appOnSurfaceA04
import com.worldtheater.archive.ui.theme.appOnSurfaceA10
import com.worldtheater.archive.ui.theme.isAppInDarkTheme
import com.worldtheater.archive.ui.widget.CHIP_TEXT_DARK
import com.worldtheater.archive.ui.widget.CHIP_TEXT_LIGHT

@Composable
fun MinimalNoteItem(
    tag: String,
    textAlign: TextAlign? = TextAlign.Center,
) {
    // Unified color: slightly stronger neutral
    val isLight = !isAppInDarkTheme()
    val containerColor =
        if (isLight) appOnSurfaceA04() else appOnSurfaceA10()
    val contentColor = if (isLight) CHIP_TEXT_LIGHT else CHIP_TEXT_DARK

    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )

    Box(
        modifier = Modifier
            .heightIn(min = 24.dp)
            .background(
                color = containerColor,
                shape = RoundedCornerShape(3.dp)
            )
            .padding(horizontal = 6.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tag,
            color = contentColor,
            textAlign = textAlign,
            style = textStyle,
        )
    }
}
