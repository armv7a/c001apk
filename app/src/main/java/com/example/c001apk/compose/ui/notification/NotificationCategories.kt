package com.example.c001apk.compose.ui.notification

import androidx.compose.runtime.Composable
import com.example.c001apk.compose.ui.component.MiuixTabRow

@Composable
internal fun NotificationCategories(
    selected: NoticeType,
    onSelected: (NoticeType) -> Unit,
) {
    MiuixTabRow(
        tabs = notificationCategories.map { it.title },
        selectedTabIndex = notificationCategories.indexOf(selected),
        onTabSelected = { onSelected(notificationCategories[it]) },
    )
}
