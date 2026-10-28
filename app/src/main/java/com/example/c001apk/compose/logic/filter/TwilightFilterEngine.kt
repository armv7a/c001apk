package com.example.c001apk.compose.logic.filter

/** Literal matching only. All groups must fit in one bounded UTF-16 window in one sentence. */
internal class TwilightFilterEngine(private val preferences: ContentFilterPreferences) {
    private data class Compiled(val rule: TwilightRule) {
        val groups = rule.groups.map { it.map(::normalizeFilterText) }
        val exceptions = rule.exceptions.map(::normalizeFilterText)
    }
    private val rules = preferences.twilight?.rules.orEmpty()
        .filter { it.key !in preferences.disabledTwilightRules }
        .map(::Compiled)

    fun match(input: FilterInput): ContentFilterMatch? {
        val sentences by lazy {
            var text = visibleFilterText(input.text)
            EXCLUDED_TERMS.forEach { text = text.replace(it, " ".repeat(it.length)) }
            text.split(Regex("[。！？!?；;\\r\\n]+"))
        }
        for (compiled in rules) {
            val rule = compiled.rule
            if (input.scope.name.lowercase() !in rule.scopes ||
                (rule.level == "strict" && !preferences.twilightStrict)) continue
            val matched = when (rule.kind) {
                "author" -> input.author in rule.values
                "topic" -> rule.values.any { it in input.topics || it in input.topicIds }
                "text" -> sentences.any { sentence ->
                    compiled.exceptions.none(sentence::contains) && matchesGroups(sentence, compiled.groups, rule.maxDistance)
                }
                else -> false
            }
            if (matched) return ContentFilterMatch(rule.key, "秋风 · ${rule.reason}")
        }
        return null
    }
}

private data class Occurrence(val start: Int, val end: Int, val group: Int)

internal fun matchesGroups(sentence: String, groups: List<List<String>>, distance: Int): Boolean {
    if (groups.isEmpty() || groups.any { group -> group.none(sentence::contains) }) return false
    val hits = mutableListOf<Occurrence>()
    groups.forEachIndexed { index, terms ->
        terms.forEach { term ->
            var start = sentence.indexOf(term)
            while (start >= 0) {
                hits += Occurrence(start, start + term.length, index)
                start = sentence.indexOf(term, start + 1)
            }
        }
    }
    val latest = IntArray(groups.size) { -1 }
    for (hit in hits.sortedBy { it.end }) {
        latest[hit.group] = maxOf(latest[hit.group], hit.start)
        val earliest = latest.min()
        if (earliest >= 0 && hit.end - earliest <= distance) return true
    }
    return false
}
