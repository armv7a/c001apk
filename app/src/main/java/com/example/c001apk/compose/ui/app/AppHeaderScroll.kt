package com.example.c001apk.compose.ui.app

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import me.onebone.toolbar.CollapsingToolbarState

/** Attach inside PullToRefresh so the header can expand before refresh consumes the remainder. */
@Composable
internal fun rememberAppHeaderScrollConnection(
    toolbarState: CollapsingToolbarState,
): NestedScrollConnection {
    val flingBehavior = ScrollableDefaults.flingBehavior()
    return remember(toolbarState, flingBehavior) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset =
                if (available.y > 0f) {
                    Offset(0f, toolbarState.dispatchRawDelta(available.y))
                } else {
                    Offset.Zero
                }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                if (available.y > 0f) {
                    val remaining = toolbarState.fling(flingBehavior, available.y)
                    Velocity(0f, available.y - remaining)
                } else {
                    Velocity.Zero
                }
        }
    }
}
