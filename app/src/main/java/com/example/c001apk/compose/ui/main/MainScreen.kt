package com.example.c001apk.compose.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.c001apk.compose.logic.model.UpdateCheckItem
import com.example.c001apk.compose.ui.component.MiuixNavigationScaffold
import com.example.c001apk.compose.ui.component.SlideTransition
import com.example.c001apk.compose.ui.component.WindowWidthSizeClass
import com.example.c001apk.compose.ui.component.rememberHapticClick
import com.example.c001apk.compose.ui.home.FollowScreen
import com.example.c001apk.compose.ui.home.HomeScreen
import com.example.c001apk.compose.ui.message.MessageScreen
import com.example.c001apk.compose.util.ReportType

/** Created by bggRGjQaUbCoE on 2024/5/30 */
@Composable
fun MainScreen(
    selectIndex: Int,
    setSelectIndex: (Int) -> Unit,
    badge: Int,
    resetBadge: () -> Unit,
    onSettingsClick: () -> Unit,
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

    val savableStateHolder = rememberSaveableStateHolder()
    val performHapticClick = rememberHapticClick {}
    var refreshState by remember { mutableStateOf(false) }
    var followRefreshState by remember { mutableStateOf(false) }

    MiuixNavigationScaffold(
        items =
            screens.mapIndexed { index, screen ->
                (if (selectIndex == index) screen.selectedIcon!! else screen.unselectedIcon!!) to
                    stringResource(screen.stringId!!)
            },
        selectedIndex = selectIndex,
        wide = widthSizeClass != WindowWidthSizeClass.Compact,
        unreadCount = badge,
        onSelect = { index ->
            performHapticClick()
            if (selectIndex == 0 && index == 0) refreshState = true
            if (selectIndex == 1 && index == 1) followRefreshState = true
            if (index == 2 && badge != 0) resetBadge()
            setSelectIndex(index)
        },
    ) { mainPadding ->
        AnimatedContent(
            modifier = Modifier.fillMaxSize(),
            label = "home-content",
            targetState = selectIndex,
            transitionSpec = {
                SlideTransition.slideLeft
                    .enterTransition()
                    .togetherWith(SlideTransition.slideLeft.exitTransition())
            },
        ) { page ->
            savableStateHolder.SaveableStateProvider(
                key = page,
                content = {
                    when (page) {
                        0 ->
                            HomeScreen(
                                bottomPadding = mainPadding.calculateBottomPadding(),
                                refreshState = refreshState,
                                onRefresh = {
                                    refreshState = true
                                },
                                resetRefreshState = {
                                    refreshState = false
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
                                bottomPadding = mainPadding.calculateBottomPadding(),
                                refreshState = followRefreshState,
                                resetRefreshState = { followRefreshState = false },
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
        setSelectIndex(0)
    }
}
