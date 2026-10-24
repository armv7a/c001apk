package com.example.c001apk.compose.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.c001apk.compose.ui.component.MiuixScaffold as Scaffold
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun MiuixNavigationScaffold(
    items: List<Pair<ImageVector, String>>,
    selectedIndex: Int,
    wide: Boolean,
    unreadCount: Int,
    onSelect: (Int) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val badge: @Composable () -> Unit = {
        if (unreadCount > 0) Badge { Text(if (unreadCount > 99) "99+" else unreadCount.toString()) }
    }
    Row(Modifier.fillMaxSize()) {
        if (wide)
            NavigationRail {
                items.forEachIndexed { index, item ->
                    NavigationRailItem(
                        selected = selectedIndex == index,
                        onClick = { onSelect(index) },
                        icon = item.first,
                        label = item.second,
                        badge = if (index == 2) badge else null,
                    )
                }
            }
        Scaffold(
            modifier = Modifier.weight(1f),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!wide)
                    NavigationBar(color = LocalBarColor.current) {
                        items.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = selectedIndex == index,
                                onClick = { onSelect(index) },
                                icon = item.first,
                                label = item.second,
                                badge = if (index == 2) badge else null,
                            )
                        }
                    }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().consumeWindowInsets(padding)) { content(padding) }
        }
    }
}
