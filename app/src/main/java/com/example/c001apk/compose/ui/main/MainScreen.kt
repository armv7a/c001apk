package com.example.c001apk.compose.ui.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.logic.model.MainNavigationOrder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.example.c001apk.compose.logic.model.UpdateCheckItem
import com.example.c001apk.compose.ui.component.MiuixNavigationScaffold
import com.example.c001apk.compose.ui.component.WindowWidthSizeClass
import com.example.c001apk.compose.ui.home.FollowScreen
import com.example.c001apk.compose.ui.home.HomeScreen
import com.example.c001apk.compose.ui.message.MessageScreen
import com.example.c001apk.compose.util.ReportType
import top.yukonga.miuix.kmp.utils.springAnimateToPage

/** Created by bggRGjQaUbCoE on 2024/5/30 */
@Composable
internal fun MainScreen(
    pageState: MainPageState,
    onSelect: (Int) -> Unit,
    badge: Int,
    onSettingsClick: () -> Unit,
    onAccountsClick: () -> Unit,
    onLocalFollowsClick: () -> Unit,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onSearch: () -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onViewApp: (String) -> Unit,
    onLogin: () -> Unit,
    onCheckUpdate: (List<UpdateCheckItem>) -> Unit,
    onViewFFFList: (String?, String) -> Unit,
    onReport: (String, ReportType) -> Unit,
    onViewNotice: (String) -> Unit,
    onViewHistory: (String) -> Unit,
    widthSizeClass: WindowWidthSizeClass,
) {

    val order = LocalUserPreferences.current.mainNavigationOrder
    val screens = order.map { listOf(Router.HOME, Router.FOLLOW, Router.PROFILE)[it] }
    val selectedPageId = pageState.selectedIndex
    val selectedPosition = order.indexOf(selectedPageId)
    val pagerState = rememberPagerState(initialPage = selectedPosition, pageCount = { screens.size })
    var displayedOrder by remember { mutableStateOf<List<Int>?>(null) }
    LaunchedEffect(selectedPosition, order) {
        // Reordering must move the existing page state with its identity, not animate through peers.
        if (displayedOrder != order) {
            pagerState.scrollToPage(selectedPosition)
            displayedOrder = order
        } else {
            pagerState.springAnimateToPage(selectedPosition)
        }
    }
    val savableStateHolder = rememberSaveableStateHolder()

    MiuixNavigationScaffold(
        items = mainNavigationItems(selectedPageId, order),
        selectedIndex = selectedPosition,
        wide = widthSizeClass != WindowWidthSizeClass.Compact,
        unreadCount = badge,
        onSelect = { position -> onSelect(order[position]) },
        showRail = false,
        badgeIndex = order.indexOf(MainNavigationOrder.PROFILE),
    ) { mainPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { screens[it].name },
            userScrollEnabled = false,
        ) { page ->
            savableStateHolder.SaveableStateProvider(
                key = screens[page].name,
                content = {
                    when (order[page]) {
                        0 ->
                            HomeScreen(
                                bottomPadding = mainPadding.calculateBottomPadding(),
                                refreshState = pageState.homeRefreshing,
                                onRefresh = {
                                    pageState.homeRefreshing = true
                                },
                                resetRefreshState = {
                                    pageState.homeRefreshing = false
                                },
                                onViewUser = onViewUser,
                                onViewFeed = onViewFeed,
                                onSearch = onSearch,
                                onOpenLink = onOpenLink,
                                onCopyText = onCopyText,
                                onViewApp = onViewApp,
                                onCheckUpdate = onCheckUpdate,
                                onReport = onReport,
                            )

                        1 ->
                            FollowScreen(
                                onManage = onLocalFollowsClick,
                                bottomPadding = mainPadding.calculateBottomPadding(),
                                refreshState = pageState.followRefreshing,
                                resetRefreshState = { pageState.followRefreshing = false },
                                onSearch = onSearch,
                                onLogin = onLogin,
                                onViewUser = onViewUser,
                                onViewFeed = onViewFeed,
                                onOpenLink = onOpenLink,
                                onCopyText = onCopyText,
                                onReport = onReport,
                            )

                        2 ->
                            MessageScreen(
                                onSettingsClick = onSettingsClick,
                                onAccountsClick = onAccountsClick,
                                bottomPadding = mainPadding.calculateBottomPadding(),
                                onLogin = onLogin,
                                onViewUser = onViewUser,
                                onViewFeed = onViewFeed,
                                onOpenLink = onOpenLink,
                                onCopyText = onCopyText,
                                onViewFFFList = onViewFFFList,
                                onReport = onReport,
                                onViewNotice = onViewNotice,
                                onViewHistory = onViewHistory,
                            )
                    }
                },
            )
        }
    }

}
