package com.example.c001apk.compose.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

enum class WindowWidthSizeClass {
    Compact,
    Medium,
    Expanded,
}

@Composable
fun currentWindowSize(): DpSize {
    // containerDpSize uses the system density, ignoring the app's content-size setting.
    val size = LocalWindowInfo.current.containerSize
    return with(LocalDensity.current) { DpSize(size.width.toDp(), size.height.toDp()) }
}

@Composable
fun currentWindowWidthSizeClass(): WindowWidthSizeClass {
    val width = currentWindowSize().width
    return when {
        width < 600.dp -> WindowWidthSizeClass.Compact
        width < 840.dp -> WindowWidthSizeClass.Medium
        else -> WindowWidthSizeClass.Expanded
    }
}
