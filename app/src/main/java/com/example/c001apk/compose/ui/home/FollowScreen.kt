package com.example.c001apk.compose.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.ui.component.MiuixActionButton
import com.example.c001apk.compose.ui.component.MiuixScaffold
import com.example.c001apk.compose.ui.component.MiuixTopBar
import com.example.c001apk.compose.ui.home.feed.HomeFeedScreen
import com.example.c001apk.compose.util.ReportType
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Search

@Composable
fun FollowScreen(
    bottomPadding: Dp,
    refreshState: Boolean,
    resetRefreshState: () -> Unit,
    onSearch: () -> Unit,
    onLogin: () -> Unit,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onReport: (String, ReportType) -> Unit,
) {
    MiuixScaffold(
        topBar = {
            MiuixTopBar(
                title = "关注",
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(MiuixIcons.Search, contentDescription = "搜索")
                    }
                },
            )
        },
    ) { padding ->
        val contentPadding =
            PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = bottomPadding + padding.calculateBottomPadding(),
            )
        if (LocalUserPreferences.current.isLogin) {
            HomeFeedScreen(
                paddingValues = contentPadding,
                type = TabType.FOLLOW,
                refreshState = refreshState,
                resetRefreshState = resetRefreshState,
                onViewUser = onViewUser,
                onViewFeed = onViewFeed,
                onOpenLink = onOpenLink,
                onCopyText = onCopyText,
                onReport = onReport,
            )
        } else {
            Box(
                Modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                MiuixActionButton(onClick = onLogin) { Text("登录后查看关注动态") }
            }
        }
    }
}
