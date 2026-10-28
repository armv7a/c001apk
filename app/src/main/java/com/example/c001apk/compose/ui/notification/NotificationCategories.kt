package com.example.c001apk.compose.ui.notification

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.c001apk.compose.ui.component.MiuixTabRow
import com.example.c001apk.compose.ui.component.TopBarScrollPage
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.utils.springAnimateToPage

@Composable
internal fun NotificationCategories(
    pagerState: PagerState,
) {
    val scope = rememberCoroutineScope()
    MiuixTabRow(
        tabs = notificationCategories.map { it.title },
        selectedTabIndex = pagerState.currentPage,
        onTabSelected = { index -> scope.launch { pagerState.springAnimateToPage(index) } },
    )
}

@Composable
internal fun NotificationPager(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    content: @Composable (NoticeType) -> Unit,
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        key = { notificationCategories[it].name },
    ) { index ->
        TopBarScrollPage(pagerState.currentPage == index) {
            content(notificationCategories[index])
        }
    }
}
