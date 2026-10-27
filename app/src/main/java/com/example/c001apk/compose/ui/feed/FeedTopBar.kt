package com.example.c001apk.compose.ui.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.example.c001apk.compose.ui.component.MiuixTopBar
import com.example.c001apk.compose.util.initialClick

/** Observe title taps before Miuix consumes them, leaving both button slots independent. */
@Composable
internal fun FeedTopBar(
    title: String,
    windowInsets: WindowInsets,
    onTitleClick: () -> Unit,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    var navigationBounds by remember { mutableStateOf(Rect.Zero) }
    var actionBounds by remember { mutableStateOf(Rect.Zero) }
    Box(
        Modifier.fillMaxWidth()
            .onGloballyPositioned { origin = it.positionInRoot() }
            .initialClick(
                label = "切换动态与评论位置",
                accepts = { local ->
                    val point = local + origin
                    point.y >= navigationBounds.top && point.y <= navigationBounds.bottom &&
                        !navigationBounds.contains(point) && !actionBounds.contains(point)
                },
                onClick = onTitleClick,
            ),
    ) {
        MiuixTopBar(
            title = title,
            windowInsets = windowInsets,
            navigationIcon = {
                Box(Modifier.onGloballyPositioned { navigationBounds = it.boundsInRoot() }) {
                    navigationIcon()
                }
            },
            actions = {
                Row(
                    Modifier.onGloballyPositioned { actionBounds = it.boundsInRoot() },
                    content = actions,
                )
            },
        )
    }
}
