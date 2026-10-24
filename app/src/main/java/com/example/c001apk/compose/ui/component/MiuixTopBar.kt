package com.example.c001apk.compose.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.SmallTopAppBar

@Composable
fun MiuixTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    windowInsets: WindowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout),
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    SmallTopAppBar(
        title = title,
        color = LocalBarColor.current,
        subtitle = subtitle,
        modifier = modifier.windowInsetsPadding(windowInsets),
        defaultWindowInsetsPadding = false,
        navigationIcon = navigationIcon,
        actions = actions,
    )
}
