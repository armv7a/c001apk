package com.example.c001apk.compose.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** One backdrop per page, shared by its bars. Nested main/page backdrops are independent. */
@Composable
fun MiuixScaffold(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = MiuixTheme.colorScheme.surface,
    contentWindowInsets: WindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
    content: @Composable (PaddingValues) -> Unit,
) {
    val backdrop = rememberBarBackdrop()
    Scaffold(
        modifier =
            modifier.windowInsetsPadding(
                WindowInsets.displayCutout
                    .union(WindowInsets.navigationBars)
                    .only(WindowInsetsSides.Horizontal),
            ),
        topBar = {
            if (topBar != null)
                BlurredBar(backdrop) {
                    CompositionLocalProvider(LocalBarColor provides Color.Transparent) { topBar() }
                }
        },
        bottomBar = {
            if (bottomBar != null)
                BlurredBar(backdrop) {
                    CompositionLocalProvider(LocalBarColor provides Color.Transparent) {
                        bottomBar()
                    }
                }
        },
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost,
        containerColor = containerColor,
        contentWindowInsets = contentWindowInsets,
    ) { padding ->
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) { content(padding) }
    }
}
