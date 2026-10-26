package com.example.c001apk.compose.logic.markdown

import java.net.URI
import java.net.URLEncoder
import java.util.IdentityHashMap
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.*
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.AttributeProvider
import org.commonmark.renderer.html.HtmlRenderer
import org.jsoup.Jsoup

class MarkdownContent(
    val source: String,
    val normalized: String,
    val document: Node?,
    val isMarkdown: Boolean,
    val html: String,
    val preview: String,
    val imageUrls: List<String>,
    val headings: Map<Node, String>,
) {
    companion object {
        private val extensions =
            listOf(
                TablesExtension.builder().maxCells(4096).build(),
                StrikethroughExtension.create(),
                TaskListItemsExtension.create(),
            )
        private val parser = Parser.builder().extensions(extensions).maxOpenBlockParsers(64).build()

        private data class Key(val text: String, val explicit: Boolean, val coolapk: Boolean)

        private val cache = LinkedHashMap<Key, MarkdownContent>()
        private var cachedChars = 0

        fun children(node: Node): List<Node> = buildList {
            var child = node.firstChild
            while (child != null) {
                add(child)
                child = child.next
            }
        }

        fun plainText(node: Node): String =
            when (node) {
                is Text -> node.literal
                is Code -> node.literal
                is FencedCodeBlock -> node.literal
                is IndentedCodeBlock -> node.literal
                is SoftLineBreak,
                is HardLineBreak -> "\n"
                is HtmlInline -> node.literal
                else ->
                    children(node).joinToString(
                        if (node is Block && node !is Paragraph && node !is Heading) "\n" else "",
                    ) {
                        plainText(it)
                    }
            }

        fun allowsUrl(value: String, image: Boolean = false): Boolean {
            if (value.isBlank() || value.any { it.code <= 32 || it.code == 127 || it == '\\' })
                return false
            val uri = runCatching { URI(value) }.getOrNull() ?: return false
            if (!uri.userInfo.isNullOrEmpty()) return false
            if (uri.scheme == "coolmarket" && !image) {
                val parts =
                    if (
                        uri.host in
                            listOf(
                                "coolapk.com",
                                "www.coolapk.com",
                                "m.coolapk.com",
                                "com.coolapk.market",
                            )
                    )
                        uri.path.trim('/').split('/')
                    else listOf(uri.host.orEmpty()) + uri.path.trim('/').split('/')
                return parts.size >= 2 &&
                    parts[0] in listOf("feed", "event", "u", "apk", "game", "t", "product") &&
                    parts[1].isNotBlank()
            }
            if (uri.scheme == null) return !image && !value.startsWith("//")
            return uri.scheme.lowercase() in listOf("http", "https") && !uri.host.isNullOrBlank()
        }

        @Synchronized
        fun parse(
            source: String,
            explicit: Boolean = false,
            coolapk: Boolean = true,
        ): MarkdownContent {
            val key = Key(source, explicit, coolapk)
            cache.remove(key)?.let {
                cache[key] = it
                return it
            }
            fun plain() =
                MarkdownContent(
                    source,
                    source,
                    null,
                    false,
                    "<p>${org.jsoup.nodes.Entities.escape(source).replace("\n", "<br>")}</p>",
                    source,
                    emptyList(),
                    emptyMap(),
                )
            if (source.length > 200000) return plain()
            val result = runCatching {
                val nativeLinks = mutableMapOf<String, String>()
                val normalized = if (coolapk) normalize(source, nativeLinks) else source
                val document = parser.parse(normalized)
                val images = mutableListOf<String>()
                val headings = IdentityHashMap<Node, String>()
                val slugs = mutableMapOf<String, Int>()
                var count = 0
                var listItems = 0
                var detected = explicit
                fun visit(node: Node, depth: Int) {
                    check(++count <= 10000 && depth <= 64)
                    if (
                        node is Heading ||
                            node is Code ||
                            node is FencedCodeBlock ||
                            node is IndentedCodeBlock ||
                            node is ThematicBreak ||
                            node is TableBlock ||
                            node is Emphasis ||
                            node is StrongEmphasis ||
                            node is Strikethrough ||
                            node is Image ||
                            node is TaskListItemMarker
                    )
                        detected = true
                    if (node is BlockQuote && Regex("(?m)^\\s*>\\s").containsMatchIn(normalized))
                        detected = true
                    if (node is ListItem) listItems++
                    if (node is Link) {
                        val label = plainText(node)
                        if (label !in nativeLinks && Regex("\\[[^]\\n]+]").containsMatchIn(source))
                            detected = true
                        if (!allowsUrl(node.destination)) node.destination = ""
                    }
                    if (node is Image) {
                        if (allowsUrl(node.destination, image = true)) images.add(node.destination)
                        else node.destination = ""
                    }
                    if (node is Text) nativeLinks[node.literal]?.let { node.literal = it }
                    children(node).forEach { visit(it, depth + 1) }
                    if (node is Heading) {
                        val slug =
                            plainText(node)
                                .trim()
                                .lowercase(java.util.Locale.ROOT)
                                .replace(Regex("[^\\p{L}\\p{N}\\p{M}_\\s-]"), "")
                                .replace(Regex("\\s"), "-")
                        val index = slugs[slug] ?: 0
                        slugs[slug] = index + 1
                        headings[node] =
                            URLEncoder.encode(
                                    if (index == 0) slug else "$slug-$index",
                                    "UTF-8",
                                )
                                .replace("+", "%20")
                    }
                }
                visit(document, 0)
                detected = detected || listItems >= 2
                val renderer =
                    HtmlRenderer.builder()
                        .extensions(extensions)
                        .escapeHtml(true)
                        .attributeProviderFactory {
                            AttributeProvider { node, _, attributes ->
                                headings[node]?.let { attributes["id"] = it }
                            }
                        }
                        .build()
                val html = renderer.render(document)
                val safe = Jsoup.parseBodyFragment(html)
                safe.outputSettings().prettyPrint(false)
                safe.select("a[href='']").forEach { it.unwrap() }
                safe.select("img[src='']").forEach {
                    it.replaceWith(org.jsoup.nodes.TextNode(it.attr("alt")))
                }
                val text = plainText(document).trim()
                MarkdownContent(
                    source,
                    nativeLinks.entries.fold(normalized) { value, (marker, label) ->
                        val escaped = buildString {
                            label.forEach { char ->
                                if (char in "\\[]`*_") append('\\')
                                append(char)
                            }
                        }
                        value.replace(marker, escaped)
                    },
                    document,
                    detected,
                    safe.body().html(),
                    text.take(240) + if (text.length > 240) "…" else "",
                    images.distinct(),
                    headings,
                )
            }
                .getOrElse { plain() }
            cache[key] = result
            cachedChars += source.length
            while (cache.size > 64 || cachedChars > 500000) {
                val first = cache.keys.first()
                cachedChars -= first.text.length
                cache.remove(first)
            }
            return result
        }

        private fun normalize(source: String, nativeLinks: MutableMap<String, String>): String {
            val input = source.replace("<!--break-->", "").replace("\r\n", "\n")
            val output = StringBuilder()
            var fenceChar: Char? = null
            var fenceLength = 0
            var inlineDelimiter: String? = null
            var offset = 0
            val fence = Regex("^ {0,3}(`{3,}|~{3,})")
            val runs = Regex("`+")
            fun normal(text: String): String {
                val anchored =
                    Regex("<a\\b[^>]*>[\\s\\S]*?</a>", RegexOption.IGNORE_CASE).replace(text) {
                        match ->
                        val anchor = Jsoup.parseBodyFragment(match.value).selectFirst("a")
                        if (
                            anchor == null ||
                                anchor.classNames().none { it.startsWith("feed-link-") }
                        )
                            match.value
                        else if (!allowsUrl(anchor.attr("href"))) anchor.text()
                        else {
                            val marker = "\uE000C001${nativeLinks.size}\uE001"
                            nativeLinks[marker] = anchor.text()
                            "[$marker](${anchor.attr("href").replace("(", "%28").replace(")", "%29")})"
                        }
                    }
                return org.jsoup.parser.Parser.unescapeEntities(
                    anchored
                        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                        .replace(Regex("</?p\\s*>", RegexOption.IGNORE_CASE), "\n\n"),
                    false,
                )
            }
            for (line in input.split('\n')) {
                val container =
                    line
                        .replace(Regex("^(?: {0,3}>[ \\t]?)+"), "")
                        .replace(Regex("^ {0,3}(?:[-+*]|[0-9]+[.)])[ \\t]+"), "")
                val opening = fence.find(container)
                if (fenceChar != null) {
                    output.appendLine(line)
                    if (
                        opening != null &&
                            opening.groupValues[1][0] == fenceChar &&
                            opening.groupValues[1].length >= fenceLength &&
                            container.substring(opening.range.last + 1).isBlank()
                    )
                        fenceChar = null
                } else if (inlineDelimiter == null && opening != null) {
                    fenceChar = opening.groupValues[1][0]
                    fenceLength = opening.groupValues[1].length
                    output.appendLine(line)
                } else if (
                    inlineDelimiter == null && (line.startsWith("    ") || line.startsWith('\t'))
                )
                    output.appendLine(line)
                else {
                    var position = 0
                    for (run in runs.findAll(line)) {
                        val text = line.substring(position, run.range.first)
                        output.append(if (inlineDelimiter == null) normal(text) else text)
                        val delimiter = run.value
                        if (inlineDelimiter == delimiter) inlineDelimiter = null
                        else if (inlineDelimiter == null) {
                            val slashes =
                                line
                                    .substring(0, run.range.first)
                                    .reversed()
                                    .takeWhile { it == '\\' }
                                    .length
                            if (
                                slashes % 2 == 0 &&
                                    Regex("(?<!`)${Regex.escape(delimiter)}(?!`)")
                                        .containsMatchIn(
                                            input.substring(offset + run.range.last + 1),
                                        )
                            )
                                inlineDelimiter = delimiter
                        }
                        output.append(delimiter)
                        position = run.range.last + 1
                    }
                    val rest = line.substring(position)
                    output.appendLine(if (inlineDelimiter == null) normal(rest) else rest)
                }
                offset += line.length + 1
            }
            return output.toString()
        }
    }
}
