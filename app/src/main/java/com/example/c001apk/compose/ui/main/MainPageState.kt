package com.example.c001apk.compose.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource

/** Shared by the outer navigation rail and the compact page's bottom navigation. */
@Stable
internal class MainPageState(initialIndex: Int = 0) {
    var selectedIndex by mutableIntStateOf(initialIndex)
    var homeRefreshing by mutableStateOf(false)
    var followRefreshing by mutableStateOf(false)

    fun select(index: Int) {
        if (selectedIndex == index) {
            if (index == 0) homeRefreshing = true
            if (index == 1) followRefreshing = true
        }
        selectedIndex = index
    }
}

@Composable
internal fun rememberMainPageState(): MainPageState = rememberSaveable(
    saver = Saver(save = { it.selectedIndex }, restore = { MainPageState(it) }),
) { MainPageState() }

@Composable
internal fun mainNavigationItems(selectedIndex: Int) =
    listOf(Router.HOME, Router.FOLLOW, Router.PROFILE).mapIndexed { index, screen ->
        (if (selectedIndex == index) screen.selectedIcon!! else screen.unselectedIcon!!) to
            stringResource(screen.stringId!!)
    }
