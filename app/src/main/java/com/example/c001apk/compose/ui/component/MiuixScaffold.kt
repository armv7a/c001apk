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
    blurBars: Boolean = true,
    containerColor: Color = MiuixTheme.colorScheme.surface,
    contentWindowInsets: WindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
    content: @Composable (PaddingValues) -> Unit,
) {
    // WebView's hardware compositor must draw once, outside the backdrop recording pass.
    val backdrop = if (blurBars) rememberBarBackdrop() else null
    Scaffold(
        modifier =
            modifier.windowInsetsPadding(
                WindowInsets.displayCutout
                    .union(WindowInsets.navigationBars)
                    .only(WindowInsetsSides.Horizontal),
            ),
        topBar = {
            if (topBar != null) {
                if (backdrop != null)
                    BlurredBar(backdrop) {
                        CompositionLocalProvider(LocalBarColor provides Color.Transparent) {
                            topBar()
                        }
                    }
                else
                    CompositionLocalProvider(LocalBarColor provides containerColor) {
                        topBar()
                    }
            }
        },
        bottomBar = {
            if (bottomBar != null) {
                if (backdrop != null)
                    BlurredBar(backdrop) {
                        CompositionLocalProvider(LocalBarColor provides Color.Transparent) {
                            bottomBar()
                        }
                    }
                else
                    CompositionLocalProvider(LocalBarColor provides containerColor) {
                        bottomBar()
                    }
            }
        },
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost,
        containerColor = containerColor,
        contentWindowInsets = contentWindowInsets,
    ) { padding ->
        Box(
            Modifier.fillMaxSize()
                .then(
                    if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier,
                ),
        ) {
            content(padding)
        }
    }
}
