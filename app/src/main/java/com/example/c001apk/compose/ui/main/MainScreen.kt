package com.example.c001apk.compose.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

    val screens =
        listOf(
            Router.HOME,
            Router.FOLLOW,
            Router.PROFILE,
        )

    val selectIndex = pageState.selectedIndex
    val pagerState = rememberPagerState(initialPage = selectIndex, pageCount = { screens.size })
    LaunchedEffect(selectIndex) { pagerState.springAnimateToPage(selectIndex) }
    val savableStateHolder = rememberSaveableStateHolder()

    MiuixNavigationScaffold(
        items = mainNavigationItems(selectIndex),
        selectedIndex = selectIndex,
        wide = widthSizeClass != WindowWidthSizeClass.Compact,
        unreadCount = badge,
        onSelect = onSelect,
        showRail = false,
    ) { mainPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { screens[it].name },
            userScrollEnabled = false,
        ) { page ->
            savableStateHolder.SaveableStateProvider(
                key = page,
                content = {
                    when (page) {
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

    BackHandler(enabled = selectIndex != 0) {
        pageState.selectedIndex = 0
    }
}
