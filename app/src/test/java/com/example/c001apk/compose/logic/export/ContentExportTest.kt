package com.example.c001apk.compose.logic.export

import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.google.gson.Gson
import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

class ContentExportTest {
    @Test
    fun exportsFullBodyMetadataAndAtMostThreeComments() {
        val feed =
            Gson()
                .fromJson(
                    """{"id":"123","username":"测试","message":"完整正文","dateline":1700000000}""",
                    HomeFeedResponse.Data::class.java,
                )
        val replies =
            (1..5).map {
                Gson()
                    .fromJson(
                        """{"id":"$it","username":"用户$it","message":"评论$it"}""",
                        HomeFeedResponse.Data::class.java,
                    )
            }
        val md = ContentExport.markdown(feed, null, replies, "评论读取不完整")
        assertTrue(md.contains("https://www.coolapk.com/feed/123"))
        assertTrue(md.contains("完整正文"))
        assertTrue(md.contains("评论3"))
        assertFalse(md.contains("评论4"))
        assertTrue(md.contains("评论读取不完整"))
    }

    @Test
    fun exportRestoresCoolapkLinksWithoutInternalPlaceholders() {
        val md = ContentExport.body("# 标题\n\n<a class=\"feed-link-uname\" href=\"/u/1\">@用户</a>")
        assertTrue(md.contains("@用户"))
        assertFalse(md.contains("C001"))
    }

    @Test
    fun offlineHtmlNeverLoadsMissingImagesOrExecutesRawHtml() {
        val md =
            "# 标题\n\n![ok](https://example.com/a.png)\n\n![missing](https://example.com/b.png)\n\n<script>alert(1)</script>\n\n[bad](javascript:alert)"
        val html =
            ContentExport.offlineHtml(
                md,
                mapOf("https://example.com/a.png" to "data:image/png;base64,AA=="),
                "一张图片未保存",
            )
        val parsed = Jsoup.parse(html)
        assertTrue(parsed.select("script,iframe,link").isEmpty())
        assertEquals(1, parsed.select("img").size)
        assertTrue(parsed.select("img").all { it.attr("src").startsWith("data:") })
        assertTrue(parsed.select("a[href='https://example.com/b.png']").isNotEmpty())
        assertTrue(parsed.select("a[href^='javascript:']").isEmpty())
        assertTrue(html.contains("default-src 'none'"))
    }
}
