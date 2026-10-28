package com.example.c001apk.compose.ui.notification

enum class NoticeType(val title: String, val url: String) {
    AT("@我的动态", "/v6/notification/atMeList"),
    COMMENT("@我的评论", "/v6/notification/atCommentMeList"),
    LIKE("我收到的赞", "/v6/notification/feedLikeList"),
    FOLLOW("好友关注", "/v6/notification/contactsFollowList"),
    MESSAGE("私信", "/v6/message/list"),
    NOTIFICATION("信息", "/v6/notification/list"),
    NOTICES("通知", "/v6/notification/list"),
}

internal val notificationCategories =
    listOf(NoticeType.NOTIFICATION, NoticeType.AT, NoticeType.COMMENT)
