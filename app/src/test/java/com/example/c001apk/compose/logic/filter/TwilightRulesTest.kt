package com.example.c001apk.compose.logic.filter

import androidx.datastore.core.DataStoreFactory
import com.example.c001apk.compose.logic.datastore.UserPreferencesSerializer
import java.nio.file.Files
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class TwilightRulesTest {
    private val textRule = TwilightRule("text.test", "测试规则", "text",
        groups = listOf(listOf("alpha", "beta"), listOf("offer")), maxDistance = 16, exceptions = listOf("refuse"))
    private val ruleSet = TwilightRuleSet(1, 2, "2026-10-26T16:41:19.710330Z", listOf(textRule))
    private fun input(text: String) = FilterInput(FilterScope.FEED, "FEED:1", "123", text)
    private fun engine(rules: TwilightRuleSet = ruleSet, strict: Boolean = false) =
        ContentFilterEngine(ContentFilterPreferences(enabled = true, twilight = rules, twilightStrict = strict))

    @Test fun currentPublicFixtureParsesWithoutBeingBundledAsDefaultRules() {
        val text = javaClass.getResource("/filters/twilight-rules-v2.json")!!.readText()
        val parsed = TwilightRuleSet.parse(text)
        assertEquals(2, parsed.revision)
        assertEquals(7, parsed.rules.size)
        assertEquals(setOf("author", "topic", "text"), parsed.rules.map { it.kind }.toSet())
        assertNull(ContentFilterPreferences.decode("").twilight)
        assertFalse(ContentFilterPreferences(twilight = parsed).enabled)
    }

    @Test fun groupedMatchesRespectDistanceOrderAndSentenceExceptions() {
        assertNotNull(engine().match(input("offer ALPHA")))
        assertNotNull(engine().match(input("beta offer")))
        assertNull(engine().match(input("alpha only")))
        assertNull(engine().match(input("alpha....................offer")))
        assertNull(engine().match(input("alpha。offer")))
        assertNull(engine().match(input("refuse alpha offer")))
        assertNotNull(engine().match(input("refuse。alpha offer")))
        assertNull(engine().match(input("<a href='alpha offer'>hello</a>")))
        assertNull(engine().match(input("<span hidden>alpha offer</span>")))
        assertNotNull(engine().match(input("alpha far far far offer then alpha")))
    }

    @Test fun strictScopesAuthorAndTopicIdsKeepUpstreamMeaning() {
        assertNull(engine(ruleSet.copy(rules = listOf(textRule.copy(level = "strict")))).match(input("alpha offer")))
        assertNotNull(engine(ruleSet.copy(rules = listOf(textRule.copy(level = "strict"))), strict = true).match(input("alpha offer")))
        val author = TwilightRule("author.test", "用户", "author", values = listOf("123"), scopes = listOf("comment"))
        val topic = TwilightRule("topic.test", "话题", "topic", values = listOf("456", "话题名称"))
        val engine = engine(ruleSet.copy(rules = listOf(author, topic)))
        assertNull(engine.match(input("123")))
        assertEquals(author.key, engine.match(input("").copy(scope = FilterScope.COMMENT))?.ruleId)
        assertEquals(topic.key, engine.match(input("").copy(topicIds = listOf("456")))?.ruleId)
        assertEquals(topic.key, engine.match(input("").copy(topics = listOf("话题名称")))?.ruleId)
    }

    @Test fun personalAndRemoteRulesShareGlobalSwitchAndContentExceptions() {
        val prefs = ContentFilterPreferences(enabled = true, twilight = ruleSet)
        assertNull(ContentFilterEngine(prefs.copy(enabled = false)).match(input("alpha offer")))
        assertNull(ContentFilterEngine(prefs.copy(allowedContent = setOf("FEED:1"))).match(input("alpha offer")))
        assertNull(ContentFilterEngine(prefs.copy(allowedAuthors = setOf("123"))).match(input("alpha offer")))
        assertNull(ContentFilterEngine(prefs.copy(disabledTwilightRules = setOf(textRule.key))).match(input("alpha offer")))
        assertNull(ContentFilterEngine(prefs.copy(scopes = setOf(FilterScope.COMMENT))).match(input("alpha offer")))
    }

    @Test fun rejectsUnsupportedOrMalformedRulesAtomically() {
        val invalid = listOf(
            ruleSet.copy(schemaVersion = 2), ruleSet.copy(revision = 0),
            ruleSet.copy(publishedAt = "2026-02-31T00:00:00Z"),
            ruleSet.copy(rules = listOf(textRule, textRule)),
            ruleSet.copy(rules = listOf(textRule.copy(id = "personal.test"))),
            ruleSet.copy(rules = listOf(textRule.copy(groups = listOf(listOf(""))))),
            ruleSet.copy(rules = listOf(textRule.copy(maxDistance = 241))),
            ruleSet.copy(rules = listOf(textRule.copy(groups = listOf(listOf("LSP"))))),
            ruleSet.copy(rules = listOf(textRule.copy(sources = listOf("http://example.com")))),
        )
        invalid.forEach { value -> assertTrue(runCatching { TwilightRuleSet.parse(Json.encodeToString(value)) }.isFailure) }
        assertTrue(runCatching { TwilightRuleSet.parse(Json.encodeToString(ruleSet).dropLast(1) + ",\"regex\":\".*\"}") }.isFailure)
    }

    @Test fun importReplacesRemoteSnapshotAndRetainsLocalChoicesAcrossDiskReopen() = runBlocking {
        val dir = Files.createTempDirectory("twilight-rules").toFile()
        val file = dir.resolve("prefs.pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val second = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val data = DataStoreFactory.create(UserPreferencesSerializer(), scope = scope, produceFile = { file })
            val store = ContentFilterStore(data)
            store.save(ContentRule("personal", "个人", terms = listOf("local")))
            store.update { it.copy(allowedContent = setOf("FEED:9")) }
            store.importTwilight(ruleSet)
            store.disable(textRule.key)
            assertFalse(store.data.first().enabled)
            val before = store.data.first()
            assertTrue(runCatching { store.importTwilight(ruleSet.copy(revision = 1)) }.isFailure)
            assertTrue(runCatching { store.importTwilight(ruleSet.copy(rules = emptyList())) }.isFailure)
            assertEquals(before, store.data.first())
            store.importTwilight(ruleSet.copy(revision = 3, rules = emptyList()))
            store.importTwilight(ruleSet.copy(revision = 4))
            val saved = store.data.first()
            assertEquals(listOf("personal"), saved.rules.map { it.id })
            assertEquals(setOf("FEED:9"), saved.allowedContent)
            assertEquals(setOf(textRule.key), saved.disabledTwilightRules)
            scope.coroutineContext[Job]!!.cancelAndJoin()
            val reopened = DataStoreFactory.create(UserPreferencesSerializer(), scope = second, produceFile = { file })
            assertEquals(saved, ContentFilterStore(reopened).data.first())
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
            second.coroutineContext[Job]!!.cancelAndJoin()
            dir.deleteRecursively()
        }
    }
}
