package com.worldtheater.archive.ui.shape

import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Global reusable shape instances
val NoteItemShape = AdaptiveSmoothShape(cornerRadius = 16.dp, limitHeight = 40.dp)

val Shapes = Shapes(
    small = SmoothRoundedCornerShape(12.dp),
    medium = SmoothRoundedCornerShape(20.dp),
    large = SmoothRoundedCornerShape(32.dp)
)