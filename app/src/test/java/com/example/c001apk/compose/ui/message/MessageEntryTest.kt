package com.example.c001apk.compose.ui.message

import com.example.c001apk.compose.ui.notification.NoticeType
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageEntryTest {
    @Test fun groupedUnreadCountDoesNotIncludeLikesFollowersOrPrivateMessages() {
        val counts = mapOf(
            NoticeType.AT to 2,
            NoticeType.COMMENT to 3,
            NoticeType.NOTIFICATION to 5,
            NoticeType.LIKE to 7,
            NoticeType.FOLLOW to 11,
            NoticeType.MESSAGE to 13,
        )
        assertEquals(listOf(10, 7, 11, 13), MessageEntry.entries.map { it.unreadCount(counts) })
        assertEquals(7, MessageEntry.NOTICES.unreadCount(counts + (NoticeType.COMMENT to null)))
    }

    @Test fun missingInvalidAndLargeUnreadCountsRemainSafe() {
        assertEquals(0, MessageEntry.NOTICES.unreadCount(emptyMap()))
        assertEquals(0, MessageEntry.NOTICES.unreadCount(mapOf(NoticeType.AT to -1)))
        assertEquals(Int.MAX_VALUE, MessageEntry.NOTICES.unreadCount(mapOf(
            NoticeType.AT to Int.MAX_VALUE,
            NoticeType.COMMENT to Int.MAX_VALUE,
        )))
    }

    @Test fun fourEntriesKeepDistinctDestinationsAndExistingDeepLinks() {
        assertEquals(
            listOf(NoticeType.NOTICES, NoticeType.LIKE, NoticeType.FOLLOW, NoticeType.MESSAGE),
            MessageEntry.entries.map { it.destination },
        )
        assertEquals("/v6/notification/atMeList", NoticeType.valueOf("AT").url)
        assertEquals("/v6/notification/atCommentMeList", NoticeType.valueOf("COMMENT").url)
        assertEquals("/v6/notification/list", NoticeType.valueOf("NOTIFICATION").url)
        assertEquals("信息", NoticeType.valueOf("NOTIFICATION").title)
        assertEquals("/v6/message/list", MessageEntry.MESSAGES.destination.url)
    }
}
