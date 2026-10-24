package com.example.c001apk.compose.logic.markdown

import org.junit.Assert.*
import org.junit.Test

class MarkdownContentTest {
    @Test
    fun ordinaryCoolapkTextIsNotMisidentified() {
        listOf("#酷安话题#", "POCO_X3_GT", "1. 单独的序号", "今天聊一下新手机").forEach {
            assertFalse(it, MarkdownContent.parse(it).isMarkdown)
        }
    }

    @Test
    fun nativeLinksRemainLinksWithoutTriggeringMarkdown() {
        val parsed =
            MarkdownContent.parse("<a class=\"feed-link-uname\" href=\"/u/123\">@测试用户</a>：你好")
        assertFalse(parsed.isMarkdown)
        assertTrue(parsed.html.contains("href=\"/u/123\""))
        assertTrue(parsed.preview.contains("@测试用户"))
        assertFalse(parsed.preview.contains("C001"))
    }

    @Test
    fun rendersSupportedBlocksAndUniqueUnicodeAnchors() {
        val parsed =
            MarkdownContent.parse(
                "# 标题\n\n# 标题\n\n- [x] 完成\n- [ ] 待办\n\n| A | B |\n| --- | --- |\n| 1 | 2 |\n\n~~删除~~ **加粗**",
            )
        assertTrue(parsed.isMarkdown)
        assertTrue(parsed.html.contains("<table>"))
        assertTrue(parsed.html.contains("type=\"checkbox\""))
        assertTrue(parsed.html.contains("<del>"))
        assertEquals(2, parsed.headings.values.toSet().size)
    }

    @Test
    fun htmlIsEscapedAndUnsafeResourcesAreRemoved() {
        val parsed =
            MarkdownContent.parse(
                "# 标题\n\n<script>alert(1)</script>\n\n[x](javascript:alert)\n\n![a](file:///etc/passwd)\n\n![b](https://example.com/b.png)",
            )
        assertFalse(parsed.html.contains("<script>"))
        assertFalse(parsed.html.contains("href=\"javascript:"))
        assertFalse(parsed.html.contains("src=\"file:"))
        assertEquals(listOf("https://example.com/b.png"), parsed.imageUrls)
        assertFalse(MarkdownContent.allowsUrl("https://user:secret@example.com/x"))
        assertFalse(MarkdownContent.allowsUrl("//example.com/x"))
    }

    @Test
    fun preservesCodeAndOnlyRemovesCoolapkBreakMarkers() {
        val raw = "# 测试\n\n```html\n<br>&amp;\n```\n\n`<p>&amp;</p>`\n\n<!--break-->"
        val parsed = MarkdownContent.parse(raw)
        assertTrue(parsed.normalized.contains("<br>&amp;"))
        assertTrue(parsed.normalized.contains("`<p>&amp;</p>`"))
        assertFalse(parsed.normalized.contains("<!--break-->"))
        assertTrue(
            MarkdownContent.parse(raw, explicit = true, coolapk = false)
                .normalized
                .contains("<!--break-->"),
        )
        assertEquals(raw, parsed.source)
    }

    @Test
    fun oversizedInputFallsBackWithoutTruncation() {
        val raw = "# a\n".repeat(60000)
        val result = MarkdownContent.parse(raw)
        assertFalse(result.isMarkdown)
        assertEquals(raw, result.source)
        assertNull(result.document)
    }
}
