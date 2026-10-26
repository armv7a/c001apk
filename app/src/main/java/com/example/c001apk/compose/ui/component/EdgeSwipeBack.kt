package com.example.c001apk.compose.ui.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlin.math.abs

/** An app edge gesture feeding the same back chain as system navigation. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun Modifier.edgeSwipeBack(enabled: Boolean): Modifier {
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    val input = remember(dispatcher) { EdgeBackInput() }
    DisposableEffect(dispatcher, input) {
        dispatcher?.addInput(input)
        onDispose {
            input.cancel()
            dispatcher?.removeInput(input)
        }
    }
    // Let the IME and text selection own gestures while editing.
    val acceptsGesture = enabled && dispatcher != null && !WindowInsets.isImeVisible
    return if (!acceptsGesture) this else pointerInput(input) {
        val edgeWidth = 32.dp.toPx()
        val minimumFlingDistance = 32.dp.toPx()
        val minimumFlingVelocity = 1000.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.isConsumed || down.position.x > edgeWidth) return@awaitEachGesture
            val velocity = VelocityTracker()
            velocity.addPosition(down.uptimeMillis, down.position)
            var dragging = false
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (change.isConsumed || event.changes.count { it.pressed } > 1) break
                    val dx = change.position.x - down.position.x
                    val dy = abs(change.position.y - down.position.y)
                    velocity.addPosition(change.uptimeMillis, change.position)
                    if (!dragging) {
                        if (!change.pressed || dx < -viewConfiguration.touchSlop) break
                        if (dy > viewConfiguration.touchSlop && dy >= dx) break
                        if (dx > viewConfiguration.touchSlop && dx > dy) {
                            input.start(down.backEvent(0f))
                            dragging = true
                        }
                    }
                    if (dragging) {
                        change.consume()
                        input.progress(change.backEvent((dx / (size.width * .5f)).coerceIn(0f, 1f)))
                        if (!change.pressed) {
                            val completed =
                                dx >= size.width * .25f ||
                                    (dx >= minimumFlingDistance &&
                                        velocity.calculateVelocity().x >= minimumFlingVelocity)
                            if (completed) input.complete()
                            break
                        }
                    }
                }
            } finally {
                // Also cancel if a system gesture takes over, a pointer disappears, or UI leaves.
                input.cancel()
            }
        }
    }
}

private fun PointerInputChange.backEvent(progress: Float) =
    NavigationEvent(
        swipeEdge = NavigationEvent.EDGE_LEFT,
        progress = progress,
        touchX = position.x.coerceAtLeast(0f),
        touchY = position.y.coerceAtLeast(0f),
        frameTimeMillis = uptimeMillis,
    )

private class EdgeBackInput : NavigationEventInput() {
    private var active = false

    fun start(event: NavigationEvent) {
        active = true
        dispatchOnBackStarted(event)
    }

    fun progress(event: NavigationEvent) {
        if (active) dispatchOnBackProgressed(event)
    }

    fun complete() {
        if (active) {
            active = false
            dispatchOnBackCompleted()
        }
    }

    fun cancel() {
        if (active) {
            active = false
            dispatchOnBackCancelled()
        }
    }
}
