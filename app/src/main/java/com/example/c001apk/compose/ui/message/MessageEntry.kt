package com.example.c001apk.compose.ui.message

import com.example.c001apk.compose.ui.notification.NoticeType
import com.example.c001apk.compose.ui.notification.notificationCategories

internal enum class MessageEntry(val title: String, val destination: NoticeType) {
    NOTICES("通知", NoticeType.NOTICES),
    LIKES("点赞", NoticeType.LIKE),
    FOLLOWS("关注", NoticeType.FOLLOW),
    MESSAGES("私信", NoticeType.MESSAGE);

    fun unreadCount(counts: Map<NoticeType, Int?>): Int {
        val types = if (this == NOTICES) notificationCategories else listOf(destination)
        return types.sumOf { (counts[it] ?: 0).coerceAtLeast(0).toLong() }
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}
