package com.example.c001apk.compose.ui.chat

import org.junit.Assert.*
import org.junit.Test

class ChatFeedLinkTest {
    @Test
    fun acceptsCanonicalAndAppLinks() {
        listOf(
                "123",
                "https://www.coolapk.com/feed/123",
                "http://coolapk.com/feed/123/",
                "coolmarket://feed/123",
                "coolmarket://www.coolapk.com/feed/123",
            )
            .forEach {
                assertEquals(it, "123", ChatFeedLink.parse(it))
            }
    }

    @Test
    fun rejectsUnrelatedAndMalformedLinks() {
        listOf(
                "https://evil.test/feed/123",
                "https://coolapk.com.evil.test/feed/123",
                "javascript:123",
                "https://www.coolapk.com/feed/abc",
                "https://www.coolapk.com/u/123",
            )
            .forEach {
                assertNull(it, ChatFeedLink.parse(it))
            }
    }
}
