package com.example.c001apk.compose.logic.filter

import java.net.URI
import java.time.OffsetDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** TG-Twilight filters/rules.json schema v1. Kept separate from editable personal rules. */
@Serializable
data class TwilightRuleSet(
    val schemaVersion: Int,
    val revision: Int,
    val publishedAt: String,
    val rules: List<TwilightRule>,
) {
    fun validate() {
        require(schemaVersion == 1 && revision > 0) { "不支持的远程规则版本" }
        validDate(publishedAt)
        require(rules.size <= 2000 && rules.map { it.id }.distinct().size == rules.size) { "远程规则重复或数量过多" }
        rules.forEach(TwilightRule::validate)
    }

    companion object {
        const val MAX_BYTES = 1024 * 1024
        fun parse(text: String): TwilightRuleSet {
            require(text.length <= MAX_BYTES) { "远程规则文件过大" }
            return Json.decodeFromString<TwilightRuleSet>(text).also { it.validate() }
        }
    }
}

@Serializable
data class TwilightRule(
    val id: String,
    val reason: String,
    val kind: String,
    val level: String = "standard",
    val scopes: List<String> = listOf("feed", "comment", "user"),
    val values: List<String> = emptyList(),
    val groups: List<List<String>> = emptyList(),
    val maxDistance: Int = 80,
    val exceptions: List<String> = emptyList(),
    val reviewedAt: String? = null,
    val sources: List<String> = emptyList(),
) {
    val key: String get() = "twilight:$id"

    fun validate() {
        validString(id, 100)
        require(id.matches(Regex("[a-zA-Z0-9_.:-]+")) && !id.startsWith("personal.")) { "远程规则 ID 无效" }
        validString(reason, 200)
        require(kind in setOf("author", "topic", "text") && level in setOf("standard", "strict"))
        require(scopes.isNotEmpty() && scopes.size <= 3 && scopes.distinct().size == scopes.size &&
            scopes.all { it in setOf("feed", "comment", "user") })
        validStrings(values, 200, 256)
        validStrings(exceptions, 16, 128)
        require(groups.size <= 8 && maxDistance in 1..240)
        groups.forEach { validStrings(it, 16, 256) }
        if (kind == "text") {
            require(values.isEmpty() && groups.isNotEmpty() && groups.all { it.isNotEmpty() })
            require(groups.flatten().all { term ->
                val normalized = normalizeFilterText(term)
                normalized.isNotEmpty() && EXCLUDED_TERMS.none(normalized::contains)
            })
        } else {
            require(values.isNotEmpty() && groups.isEmpty() && exceptions.isEmpty() && level == "standard")
            if (kind == "author") require(values.all { it.matches(Regex("[1-9][0-9]{0,19}")) })
        }
        reviewedAt?.let(::validDate)
        validStrings(sources, 8, 2048)
        sources.forEach { source ->
            val uri = URI(source)
            require(uri.scheme == "https" && !uri.host.isNullOrEmpty() && uri.rawUserInfo == null)
        }
    }
}

internal val EXCLUDED_TERMS = listOf("lsp", "手冲", "后入", "性压抑")

private fun validString(value: String, max: Int) {
    require(value.isNotEmpty() && value.length <= max && value.trim() == value &&
        value.none { it.code < 32 || it.code == 127 }) { "远程规则格式无效" }
}

private fun validStrings(values: List<String>, maxCount: Int, maxLength: Int) {
    require(values.size <= maxCount && values.distinct().size == values.size)
    values.forEach { validString(it, maxLength) }
}

private fun validDate(value: String) {
    require(value.length <= 40 && value.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,6})?(Z|[+-]\\d{2}:\\d{2})")))
    OffsetDateTime.parse(value)
}
