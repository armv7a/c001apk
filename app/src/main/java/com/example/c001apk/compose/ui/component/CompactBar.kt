package com.example.c001apk.compose.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val LocalCompactBar = staticCompositionLocalOf { false }

/** Scale native bar geometry without shrinking text a second time after the app theme. */
@Composable
internal fun CompactBar(content: @Composable () -> Unit) {
    if (LocalCompactBar.current) {
        content()
        return
    }
    val density = LocalDensity.current
    val scale = 0.95f
    CompositionLocalProvider(
        LocalCompactBar provides true,
        LocalDensity provides Density(density.density * scale, density.fontScale / scale),
        content = content,
    )
}
