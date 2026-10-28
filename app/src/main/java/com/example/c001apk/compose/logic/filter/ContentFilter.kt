package com.example.c001apk.compose.logic.filter

import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.markdown.MarkdownContent
import java.text.Normalizer
import java.util.Locale
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.jsoup.Jsoup

@Serializable
enum class FilterKind(val title: String) { TEXT("关键词"), AUTHOR("用户"), TOPIC("话题") }

@Serializable
enum class FilterScope(val title: String) { FEED("动态"), COMMENT("评论"), USER("用户卡片") }

@Serializable
data class ContentRule(
    val id: String,
    val title: String,
    val kind: FilterKind = FilterKind.TEXT,
    val terms: List<String>,
    val exceptions: List<String> = emptyList(),
    val scopes: Set<FilterScope> = FilterScope.entries.toSet(),
    val enabled: Boolean = true,
) {
    fun validate() {
        require(id.isNotBlank() && id.length <= 100 && title.isNotBlank() && title.length <= 80) { "请填写规则名称" }
        require(terms.isNotEmpty() && terms.size <= 50 && exceptions.size <= 50) { "匹配词需为 1 至 50 项" }
        require((terms + exceptions).all { normalizeFilterText(it).isNotBlank() && it.length <= 128 }) { "每项需为 1 至 128 个可见字符" }
        require(scopes.isNotEmpty()) { "请至少选择一个生效范围" }
        if (kind == FilterKind.AUTHOR) require(terms.all { it.matches(Regex("[1-9][0-9]{0,19}")) }) { "用户规则请填写数字 UID" }
    }
}

@Serializable
data class ContentFilterPreferences(
    val enabled: Boolean = false,
    val scopes: Set<FilterScope> = FilterScope.entries.toSet(),
    val rules: List<ContentRule> = emptyList(),
    val allowedAuthors: Set<String> = emptySet(),
    val allowedContent: Set<String> = emptySet(),
    val twilight: TwilightRuleSet? = null,
    val twilightStrict: Boolean = false,
    val disabledTwilightRules: Set<String> = emptySet(),
) {
    fun encode(): String {
        require(rules.size <= 200 && rules.map { it.id }.distinct().size == rules.size) { "最多保存 200 条不同规则" }
        require(allowedAuthors.size <= 1000 && allowedContent.size <= 1000) { "例外已达上限，请先清理" }
        rules.forEach { it.validate() }
        twilight?.validate()
        require(disabledTwilightRules.size <= 10000 && disabledTwilightRules.all { it.startsWith("twilight:") && it.length <= 109 })
        require(codec.encodeToString(copy(twilight = null, twilightStrict = false, disabledTwilightRules = emptySet())).length <= 512 * 1024) {
            "个人规则内容过多，请减少匹配词或例外"
        }
        return codec.encodeToString(this).also {
            require(it.length <= 2 * 1024 * 1024) { "规则内容过多，请减少匹配词或例外" }
        }
    }

    companion object {
        private val codec = Json { ignoreUnknownKeys = true }
        fun decode(value: String): ContentFilterPreferences = runCatching {
            require(value.length <= 2 * 1024 * 1024)
            codec.decodeFromString<ContentFilterPreferences>(value).also { it.encode() }
        }.getOrDefault(ContentFilterPreferences())
    }
}

data class FilterInput(
    val scope: FilterScope,
    val key: String?,
    val author: String?,
    val text: String,
    val topics: List<String> = emptyList(),
    val topicIds: List<String> = emptyList(),
)

data class ContentFilterMatch(val ruleId: String, val reason: String)

/** Public TG-Twilight filtering semantics, implemented with native models; no remote regex. */
class ContentFilterEngine(private val preferences: ContentFilterPreferences) {
    val isEnabled = preferences.enabled && preferences.scopes.isNotEmpty() &&
        (preferences.rules.any { it.enabled } || preferences.twilight?.rules?.isNotEmpty() == true)
    private val twilight by lazy { TwilightFilterEngine(preferences) }
    private val rules by lazy {
        preferences.rules.filter { it.enabled }.map { rule ->
            Triple(rule, rule.terms.map(::normalizeFilterText), rule.exceptions.map(::normalizeFilterText))
        }
    }

    fun match(input: FilterInput): ContentFilterMatch? {
        if (!isEnabled || input.scope !in preferences.scopes ||
            input.author in preferences.allowedAuthors || input.key in preferences.allowedContent) return null
        val sentences by lazy { visibleFilterText(input.text).split(Regex("[。！？!?；;\\r\\n]+")) }
        for ((rule, terms, exceptions) in rules) {
            if (input.scope !in rule.scopes) continue
            val matched = when (rule.kind) {
                FilterKind.AUTHOR -> input.author in rule.terms
                FilterKind.TOPIC -> input.topics.any { normalizeFilterText(it) in terms }
                FilterKind.TEXT -> sentences.any { sentence ->
                    exceptions.none(sentence::contains) && terms.any(sentence::contains)
                }
            }
            if (matched) return ContentFilterMatch(rule.id, rule.title)
        }
        return twilight.match(input)
    }

    fun match(item: HomeFeedResponse.Data, scope: FilterScope? = null): ContentFilterMatch? =
        if (isEnabled) item.filterInput(scope)?.let(::match) else null
}

fun normalizeFilterText(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
        .replace(Regex("[\\u200b-\\u200f\\u202a-\\u202e\\u2060\\ufeff]"), "")
        .replace(Regex("[\\t \\x0B\\f]+"), " ").trim()

fun visibleFilterText(value: String): String {
    val markdown = MarkdownContent.parse(value)
    val document = Jsoup.parseBodyFragment(if (markdown.isMarkdown) markdown.html else value)
    document.select("script,style,template,noscript,[hidden],[aria-hidden=true]").remove()
    document.select("br,p,div,li,blockquote,h1,h2,h3").forEach { it.appendText("\n") }
    return normalizeFilterText(document.body().wholeText())
}

fun HomeFeedResponse.Data.filterInput(scope: FilterScope? = null): FilterInput? {
    val resolved = scope ?: when (entityType) {
        "feed" -> FilterScope.FEED
        "feed_reply" -> FilterScope.COMMENT
        "user" -> FilterScope.USER
        else -> return null // Messages, notices and server control cards are never filtered as posts.
    }
    return FilterInput(
        resolved, id?.takeIf(String::isNotBlank)?.let { "${resolved.name}:$it" },
        uid?.takeIf(String::isNotBlank) ?: id.takeIf { resolved == FilterScope.USER },
        if (resolved == FilterScope.USER) listOfNotNull(username, bio, description).joinToString("\n")
        else listOfNotNull(messageTitle, title, message, articleFilterText(messageRawOutput)).joinToString("\n"),
        tags.orEmpty().split(',', '#').map(String::trim).filter(String::isNotBlank) +
            listOfNotNull(ttitle) + relationRows.orEmpty().mapNotNull { it.title },
        relationRows.orEmpty().filter { it.entityType == "topic" }.mapNotNull { it.id },
    )
}

/** Only rendered fields count; image URLs and article metadata must not trigger text rules. */
internal fun articleFilterText(raw: String?): String? = runCatching {
    if (raw.isNullOrBlank()) return null
    val rows = Json.parseToJsonElement(raw) as? JsonArray ?: return null
    rows.mapNotNull { it as? JsonObject }.joinToString("\n") { row ->
        fun text(key: String) = (row[key] as? JsonPrimitive)?.contentOrNull
        when (text("type")) {
            "text" -> text("message").orEmpty()
            "image" -> text("description").orEmpty()
            "shareUrl" -> listOfNotNull(text("title"), text("subTitle")).joinToString("\n")
            else -> ""
        }
    }
}.getOrNull()

fun HomeFeedResponse.ForwardSourceFeed.filterInput() = FilterInput(
    FilterScope.FEED, id?.takeIf(String::isNotBlank)?.let { "FEED:$it" }, uid,
    listOfNotNull(messageTitle, message).joinToString("\n"),
)
