package com.example.c001apk.compose.util

import org.junit.Assert.*
import org.junit.Test

class CoolapkLinkTest {
    @Test
    fun acceptsBothCoolapkAndCoolapk1s() {
        for (host in CoolapkLink.hosts) for (scheme in listOf("http", "https")) {
            val link = CoolapkLink.parse("$scheme://$host/feed/123?shareUid=999#comments")!!
            assertEquals(listOf("feed", "123"), link.segments)
        }
        assertNotNull(CoolapkLink.parse("https://coolapk1s.com/event/123"))
        assertNotNull(CoolapkLink.parse("https://www.coolapk1s.com/event/123"))
    }

    @Test
    fun handlesOfficialAndRouteAuthoritySchemes() {
        for (url in
            listOf(
                "coolmarket://feed/123",
                "coolmarket://www.coolapk.com/feed/123",
                "coolmarket://com.coolapk.market/feed/123",
            )) assertEquals(listOf("feed", "123"), CoolapkLink.parse(url)!!.segments)
    }

    @Test
    fun preservesEncodedTopicsAndSeparatesSearchQuery() {
        assertEquals(
            listOf("t", "C++"),
            CoolapkLink.parse("https://coolapk.com/t/C%2B%2B?type=8")!!.segments,
        )
        assertEquals(
            "a b",
            CoolapkLink.parse("coolmarket://com.coolapk.market/search?searchValue=a+b")!!
                .query["searchValue"],
        )
    }

    @Test
    fun rejectsHostConfusionCredentialsAndInvalidInput() {
        for (url in
            listOf(
                "https://coolapk.com.example.com/feed/123",
                "https://notcoolapk1s.com/feed/123",
                "https://coolapk.com@evil.example/feed/123",
                "file:///feed/123",
                "/feed/%ZZ",
                "/feed/%2F123",
            )) assertNull(url, CoolapkLink.parse(url))
    }

    @Test
    fun extractsFirstSupportedSharedLink() {
        assertEquals(
            "https://www.coolapk1s.com/feed/123",
            CoolapkLink.fromSharedText(
                "分享内容\nhttps://example.com/no\nhttps://www.coolapk1s.com/feed/123\n分享自c001apk",
            ),
        )
    }

    @Test
    fun shareContainsBothDomainsAndKeepsCopySingleLink() {
        assertEquals(4, ShareContent.text(ShareType.TOPIC, "C++", "<b>标题</b>").lines().size)
        assertTrue(
            ShareContent.text(ShareType.TOPIC, "C++", "标题")
                .contains("https://www.coolapk1s.com/t/C%2B%2B"),
        )
        assertEquals("https://www.coolapk1s.com/feed/123", ShareContent.link(ShareType.FEED, "123"))
    }
}
