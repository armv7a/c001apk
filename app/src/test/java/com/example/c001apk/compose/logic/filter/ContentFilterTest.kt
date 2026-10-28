package com.example.c001apk.compose.logic.filter

import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class ContentFilterTest {
    private val rule = ContentRule("r1", "促销", terms = listOf("SALE"), exceptions = listOf("科普"))
    private fun prefs(vararg rules: ContentRule) = ContentFilterPreferences(enabled = true, rules = rules.toList())
    private fun input(text: String, scope: FilterScope = FilterScope.FEED) = FilterInput(scope, "FEED:1", "123", text)

    @Test fun defaultsAreOffAndNeverHideMessagesOrControlCards() {
        assertNull(ContentFilterEngine(ContentFilterPreferences(rules = listOf(rule))).match(input("sale")))
        for (type in listOf("message", "notification", "card")) {
            val item = Gson().fromJson("""{"entityType":"$type","message":"sale"}""", HomeFeedResponse.Data::class.java)
            assertNull(ContentFilterEngine(prefs(rule)).match(item))
        }
    }

    @Test fun matchesVisibleTextWithoutHtmlAttributesOrInvisiblePayloads() {
        val engine = ContentFilterEngine(prefs(rule))
        assertNotNull(engine.match(input("<b>ＳＡ\u200bＬＥ</b>")))
        assertNull(engine.match(input("<a href='https://sale.example' title='sale'>普通内容</a><script>sale</script><span hidden>sale</span>")))
        assertNull(engine.match(input("[普通内容](https://sale.example/image.jpg)")))
        assertNotNull(engine.match(input("<p>sale 科普</p><p>sale 优惠</p>")))
    }

    @Test fun articleBodyMatchesButImageUrlsDoNot() {
        val article = """[{"type":"text","message":"正文 sale"},{"type":"image","url":"https://image.example","description":"配图"}]"""
        assertNotNull(ContentFilterEngine(prefs(rule)).match(input(articleFilterText(article)!!)))
        val image = """[{"type":"image","url":"https://sale.example","description":"配图"}]"""
        assertNull(ContentFilterEngine(prefs(rule)).match(input(articleFilterText(image)!!)))
        assertNull(articleFilterText("invalid JSON"))
    }

    @Test fun exceptionsOnlyExemptTheSameSentence() {
        val engine = ContentFilterEngine(prefs(rule))
        assertNull(engine.match(input("SALE 科普。普通内容")))
        assertNotNull(engine.match(input("科普。sale")))
        assertNotNull(engine.match(input("SALE 科普！sale")))
        assertNull(engine.match(input("售卖")))
    }

    @Test fun scopeAndAllowListsTakePrecedenceAndDisabledRulesRestoreContent() {
        val settings = prefs(rule.copy(scopes = setOf(FilterScope.COMMENT)))
        assertNull(ContentFilterEngine(settings).match(input("sale")))
        assertNotNull(ContentFilterEngine(settings).match(input("sale", FilterScope.COMMENT)))
        assertNull(ContentFilterEngine(settings.copy(scopes = setOf(FilterScope.FEED))).match(input("sale", FilterScope.COMMENT)))
        assertNull(ContentFilterEngine(prefs(rule).copy(allowedContent = setOf("FEED:1"))).match(input("sale")))
        assertNull(ContentFilterEngine(prefs(rule).copy(allowedAuthors = setOf("123"))).match(input("sale")))
        assertNull(ContentFilterEngine(prefs(rule.copy(enabled = false))).match(input("sale")))
    }

    @Test fun authorAndTopicRulesUseExactIdentifiers() {
        val author = rule.copy(kind = FilterKind.AUTHOR, terms = listOf("123"))
        assertNotNull(ContentFilterEngine(prefs(author)).match(input("normal")))
        assertNull(ContentFilterEngine(prefs(author)).match(input("normal").copy(author = "1234")))
        val topic = rule.copy(kind = FilterKind.TOPIC, terms = listOf("手机"))
        assertNotNull(ContentFilterEngine(prefs(topic)).match(input("normal").copy(topics = listOf(" 手机 "))))
        assertNull(ContentFilterEngine(prefs(topic)).match(input("手机").copy(topics = listOf("手机摄影"))))
    }

    @Test fun serializationRetainsRulesAndRejectsInvalidConfiguration() {
        val settings = prefs(rule).copy(allowedContent = setOf("FEED:2"))
        assertEquals(settings, ContentFilterPreferences.decode(settings.encode()))
        assertFalse(ContentFilterPreferences.decode("{bad json}").enabled)
        assertTrue(runCatching { prefs(rule, rule).encode() }.isFailure)
        assertTrue(runCatching { prefs(rule.copy(terms = emptyList())).encode() }.isFailure)
        assertTrue(runCatching { prefs(rule.copy(terms = listOf("\u200b"))).encode() }.isFailure)
        val oversized = ContentFilterPreferences(rules = (1..200).map { rule.copy(id = "$it", terms = List(50) { "词".repeat(128) }) })
        assertTrue(runCatching { oversized.encode() }.isFailure)
        assertTrue(runCatching { prefs(rule.copy(kind = FilterKind.AUTHOR, terms = listOf("username"))).encode() }.isFailure)
    }
}
