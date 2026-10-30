package com.example.c001apk.compose.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource

/** Selection is the stable page ID (Home=0, Follow=1, Profile=2), not its displayed position. */
@Stable
internal class MainPageState(initialIndex: Int = 0) {
    var selectedIndex by mutableIntStateOf(initialIndex)
    var homeRefreshing by mutableStateOf(false)
    var followRefreshing by mutableStateOf(false)

    fun select(index: Int, detailNavigator: AppNavigator) {
        if (selectedIndex != index) {
            while (detailNavigator.canGoBack) detailNavigator.popBackStack()
        }
        select(index)
    }

    fun select(index: Int) {
        if (selectedIndex == index) {
            if (index == 0) homeRefreshing = true
            if (index == 1) followRefreshing = true
        }
        selectedIndex = index
    }
}

@Composable
internal fun MainPageBackHandler(
    navigator: AppNavigator,
    pageState: MainPageState,
    detailVisible: Boolean,
    onSelect: (Int) -> Unit,
) {
    BackHandler(
        enabled = navigator.currentDestination == AppDestination.Main &&
            pageState.selectedIndex != 0 && !detailVisible,
    ) { onSelect(0) }
}

@Composable
internal fun rememberMainPageState(): MainPageState = rememberSaveable(
    saver = Saver(save = { it.selectedIndex }, restore = { MainPageState(it) }),
) { MainPageState() }

@Composable
internal fun mainNavigationItems(
    selectedIndex: Int,
    order: List<Int> = com.example.c001apk.compose.logic.model.MainNavigationOrder.default,
) = order.map { pageId ->
    val screen = listOf(Router.HOME, Router.FOLLOW, Router.PROFILE)[pageId]
    (if (selectedIndex == pageId) screen.selectedIcon!! else screen.unselectedIcon!!) to
        stringResource(screen.stringId!!)
}
