package com.example.c001apk.compose.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import top.yukonga.miuix.kmp.nav.core.NavController
import top.yukonga.miuix.kmp.nav.core.rememberNavController

/** App-specific actions over Miuix's persisted back stack. */
@Stable
class AppNavigator(val controller: NavController) {
    val currentEntry: AppNavEntry get() = controller.backStack.last() as AppNavEntry
    val currentDestination: AppDestination get() = currentEntry.destination
    val canGoBack: Boolean get() = controller.backStack.size > 1

    fun navigate(destination: AppDestination) {
        // Ignore a second tap on the already-opening page, while allowing repeated visits later.
        if (currentDestination != destination) controller.push(AppNavEntry(destination))
    }

    fun popBackStack(): Boolean = controller.pop()

    /** Keep a bounded native back stack so each reading entry retains its own saved state. */
    fun navigateReading(destination: AppDestination.Feed, historyLimit: Int = 8) {
        require(historyLimit > 0)
        if (currentDestination == destination) return
        Snapshot.withMutableSnapshot {
            while (controller.backStack.size > historyLimit) controller.backStack.removeAt(1)
            controller.push(AppNavEntry(destination))
        }
    }

    fun replace(destination: AppDestination) {
        if (currentDestination != destination) controller.replace(AppNavEntry(destination))
    }

    fun reset(destination: AppDestination = AppDestination.Main) {
        Snapshot.withMutableSnapshot {
            controller.backStack.clear()
            controller.push(AppNavEntry(destination))
        }
    }
}

@Composable
fun rememberAppNavigator(root: AppDestination = AppDestination.Main): AppNavigator =
    key("miuix-navigation-v1") {
        val controller = rememberNavController<AppNavEntry>(AppNavEntry(root))
        remember(controller) { AppNavigator(controller) }
    }
