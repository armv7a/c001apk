package com.example.c001apk.compose.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
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
    blurBars: Boolean = LocalUserPreferences.current.blurBars,
    floatingBottomBar: Boolean = false,
    containerColor: Color = MiuixTheme.colorScheme.surface,
    contentWindowInsets: WindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
    content: @Composable (PaddingValues) -> Unit,
) {
    // WebView's hardware compositor must draw once, outside the backdrop recording pass.
    val backdrop = if (blurBars) rememberBarBackdrop() else null
    val collapseTabs = LocalUserPreferences.current.collapseTabsOnScroll
    val tabScrollThreshold = with(LocalDensity.current) { 12.dp.toPx() }
    val tabState = remember(collapseTabs, tabScrollThreshold) {
        if (collapseTabs) CollapsingTabsState(tabScrollThreshold) else null
    }
    CompositionLocalProvider(LocalCollapsingTabs provides tabState) {
        Scaffold(
            modifier =
                modifier.then(if (tabState != null) Modifier.nestedScroll(tabState) else Modifier)
                    .windowInsetsPadding(
                        WindowInsets.displayCutout
                            .union(WindowInsets.navigationBars)
                            .only(WindowInsetsSides.Horizontal),
                    ),
            topBar = {
                if (topBar != null) {
                    if (backdrop != null)
                        BlurredBar(backdrop) {
                            CompositionLocalProvider(LocalBarColor provides Color.Transparent) {
                                CompactBar { topBar() }
                            }
                        }
                    else
                        CompositionLocalProvider(LocalBarColor provides containerColor) {
                            Box(Modifier.background(containerColor)) { CompactBar { topBar() } }
                        }
                }
            },
            bottomBar = {
                if (bottomBar != null) {
                    if (floatingBottomBar)
                        // The floating bar draws its own rounded surface; keep the outer gaps clear.
                        CompositionLocalProvider(LocalBarBackdrop provides backdrop) {
                            CompactBar { bottomBar() }
                        }
                    else if (backdrop != null)
                        BlurredBar(backdrop) {
                            CompositionLocalProvider(LocalBarColor provides Color.Transparent) {
                                CompactBar { bottomBar() }
                            }
                        }
                    else
                        CompositionLocalProvider(LocalBarColor provides containerColor) {
                            Box(Modifier.background(containerColor)) { CompactBar { bottomBar() } }
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
}
