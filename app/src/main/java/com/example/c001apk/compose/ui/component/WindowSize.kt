package com.example.c001apk.compose.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

enum class WindowWidthSizeClass {
    Compact,
    Medium,
    Expanded,
}

@Composable
fun currentWindowWidthSizeClass(): WindowWidthSizeClass {
    val width = LocalWindowInfo.current.containerDpSize.width
    return when {
        width < 600.dp -> WindowWidthSizeClass.Compact
        width < 840.dp -> WindowWidthSizeClass.Medium
        else -> WindowWidthSizeClass.Expanded
    }
}
