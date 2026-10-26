package com.example.c001apk.compose.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Inset the list's initial content, allowing scrolled items to pass beneath the tab bar. */
internal val LocalTabContentPadding = staticCompositionLocalOf { PaddingValues() }

/** The enclosing bar provides the blur; the native tab surfaces must let it show through. */
@Composable
fun MiuixTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
) {
    AnimatedVisibility(
        visible = LocalCollapsingTabs.current?.visible ?: true,
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top),
    ) {
        TabRow(
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
            modifier = modifier,
            indication = LocalIndication.current,
            colors =
                TabRowDefaults.tabRowColors(
                    backgroundColor = Color.Transparent,
                    selectedBackgroundColor =
                        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
                ),
        )
    }
}

/** For tabs below a page header, record only the pager as the source of their native miuix blur. */
@Composable
fun MiuixTabScaffold(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    visible: Boolean = true,
    belowTabs: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    if (!visible) {
        content()
        return
    }
    MiuixScaffold(
        topBar = {
            Column {
                MiuixTabRow(
                    tabs = tabs,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = onTabSelected,
                )
                belowTabs()
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        CompositionLocalProvider(LocalTabContentPadding provides padding) {
            content()
        }
    }
}
