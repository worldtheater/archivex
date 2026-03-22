package com.worldtheater.archive.ui.theme

import androidx.compose.material3.Typography

import androidx.compose.ui.unit.sp

val defaultTypography = Typography()

val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontSize = 38.sp),
    displayMedium = defaultTypography.displayMedium.copy(fontSize = 32.sp),
    displaySmall = defaultTypography.displaySmall.copy(fontSize = 26.sp),
    headlineLarge = defaultTypography.headlineLarge.copy(fontSize = 26.sp),
    headlineMedium = defaultTypography.headlineMedium.copy(fontSize = 24.sp),
    headlineSmall = defaultTypography.headlineSmall.copy(fontSize = 22.sp),
    titleLarge = defaultTypography.titleLarge.copy(fontSize = 20.sp),
    titleMedium = defaultTypography.titleMedium.copy(fontSize = 18.sp),
    titleSmall = defaultTypography.titleSmall.copy(fontSize = 16.sp),
    bodyLarge = defaultTypography.bodyLarge.copy(fontSize = 16.sp),
    bodyMedium = defaultTypography.bodyMedium.copy(fontSize = 15.sp),
    bodySmall = defaultTypography.bodySmall.copy(fontSize = 13.sp),
    labelLarge = defaultTypography.labelLarge.copy(fontSize = 14.sp),
    labelMedium = defaultTypography.labelMedium.copy(fontSize = 12.sp),
    labelSmall = defaultTypography.labelSmall.copy(fontSize = 11.sp)
)