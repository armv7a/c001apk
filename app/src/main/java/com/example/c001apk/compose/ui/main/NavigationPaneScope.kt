package com.example.c001apk.compose.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

/** Keep a hidden pane's state alive without letting it resume or intercept the visible pane's back. */
@Composable
internal fun NavigationPaneScope(active: Boolean, content: @Composable () -> Unit) {
    val lifecycle = rememberLifecycleOwner(
        maxLifecycle = if (active) Lifecycle.State.RESUMED else Lifecycle.State.CREATED,
    )
    val parent = LocalNavigationEventDispatcherOwner.current
    if (parent == null) {
        CompositionLocalProvider(LocalLifecycleOwner provides lifecycle, content = content)
        return
    }
    val dispatcher = remember(parent) {
        NavigationEventDispatcher(parent = parent.navigationEventDispatcher).also { it.isEnabled = active }
    }
    val owner = remember(dispatcher) {
        object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher = dispatcher
        }
    }
    SideEffect {
        try {
            dispatcher.isEnabled = active
        } catch (_: IllegalStateException) {
            // A parent dispatcher may already have cascade-disposed this scope during teardown.
        }
    }
    DisposableEffect(dispatcher) {
        onDispose {
            try {
                dispatcher.dispose()
            } catch (_: IllegalStateException) {
                // Miuix entry dispatchers can be disposed by their ancestor first.
            }
        }
    }
    CompositionLocalProvider(
        LocalLifecycleOwner provides lifecycle,
        LocalNavigationEventDispatcherOwner provides owner,
        content = content,
    )
}
