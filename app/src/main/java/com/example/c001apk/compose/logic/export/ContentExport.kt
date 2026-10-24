package com.example.c001apk.compose.logic.export

import com.example.c001apk.compose.logic.markdown.MarkdownContent
import com.example.c001apk.compose.logic.model.FeedArticleContentBean
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/** Export is a snapshot; it never changes the stored post, current sort or comment cursor. */
object ContentExport {
    fun escape(text: String) = text.replace(Regex("([\\\\`*_{}\\[\\]()#+.!|>~-])"), "\\\\$1")

    fun text(html: String?) = Jsoup.parseBodyFragment(html.orEmpty()).text()

    fun body(source: String): String {
        val parsed = MarkdownContent.parse(source)
        if (parsed.isMarkdown) return parsed.normalized
        fun convert(node: Node): String =
            when (node) {
                is TextNode -> escape(node.wholeText)
                is Element -> {
                    val inner = node.childNodes().joinToString("") { convert(it) }
                    when (node.normalName()) {
                        "br" -> "\n"
                        "p",
                        "div" -> "$inner\n\n"
                        "a" ->
                            if (MarkdownContent.allowsUrl(node.attr("href"))) {
                                val url = resolve(node.attr("href"))
                                "[$inner](${url.replace("(", "%28").replace(")", "%29")})"
                            } else inner
                        "img" -> image(node.attr("src"), node.attr("alt"))
                        "script",
                        "style",
                        "iframe" -> ""
                        else -> inner
                    }
                }
                else -> ""
            }
        return Jsoup.parseBodyFragment(source.replace("<!--break-->", ""))
            .body()
            .childNodes()
            .joinToString("") { convert(it) }
    }

    private fun resolve(url: String): String =
        if (java.net.URI(url).scheme == null)
            java.net.URI("https://www.coolapk.com/").resolve(url).toString()
        else url

    private fun image(url: String, alt: String = "图片"): String =
        if (MarkdownContent.allowsUrl(url, image = true))
            "\n\n![${escape(alt)}](${url.replace("(", "%28").replace(")", "%29")})\n\n"
        else ""

    private fun time(seconds: Long?): String =
        seconds?.let {
            runCatching {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochSecond(it))
            }
                .getOrNull()
        } ?: "时间未知"

    fun markdown(
        feed: HomeFeedResponse.Data,
        article: List<FeedArticleContentBean>?,
        comments: List<HomeFeedResponse.Data>,
        note: String?,
    ): String = buildString {
        appendLine(
            "# ${escape(text(feed.messageTitle ?: feed.title).ifBlank { "动态 ${feed.id.orEmpty()}" })}\n",
        )
        appendLine("作者：${escape(text(feed.username))}  ")
        appendLine("发布时间：${time(feed.dateline)}  ")
        appendLine("来源：https://www.coolapk.com/feed/${feed.id}\n")
        if (!article.isNullOrEmpty()) {
            feed.messageCover?.let { appendLine(image(it, "封面")) }
            article.forEach {
                when (it.type) {
                    "text" -> appendLine(body(it.message.orEmpty()))
                    "image" -> appendLine(image(it.url.orEmpty(), it.description.orEmpty()))
                    "shareUrl" ->
                        if (MarkdownContent.allowsUrl(it.url.orEmpty()))
                            appendLine("[${escape(text(it.title))}](${resolve(it.url.orEmpty())})")
                }
                appendLine()
            }
        } else {
            appendLine(body(feed.message.orEmpty()))
            feed.picArr.orEmpty().forEach { appendLine(image(it)) }
        }
        feed.forwardSourceFeed?.let {
            appendLine("\n## 转发内容\n")
            appendLine("作者：${escape(text(it.username))}\n")
            if (!it.messageTitle.isNullOrBlank())
                appendLine("### ${escape(text(it.messageTitle))}\n")
            appendLine(body(it.message.orEmpty()))
            it.picArr.orEmpty().forEach { url -> appendLine(image(url)) }
            if (MarkdownContent.allowsUrl(it.url.orEmpty()))
                appendLine("\n原文：${resolve(it.url.orEmpty())}")
        }
        appendLine("\n---\n\n## 评论（最多三条）\n")
        note?.let { appendLine("> ${escape(it)}\n") }
        comments.take(3).forEach {
            appendLine("### ${escape(text(it.username))}\n")
            appendLine("${time(it.dateline)}\n")
            appendLine(body(it.message.orEmpty()))
            it.picArr.orEmpty().forEach { url -> appendLine(image(url)) }
            appendLine()
        }
        if (comments.isEmpty() && note == null) appendLine("暂无可导出的评论。")
    }

    fun offlineHtml(markdown: String, images: Map<String, String>, note: String?): String {
        val content = MarkdownContent.parse(markdown, explicit = true, coolapk = false)
        val doc = Jsoup.parseBodyFragment(content.html)
        doc.select("img").forEach { img ->
            val url = img.attr("src")
            val data = images[url]
            if (data != null) img.attr("src", data)
            else {
                val replacement = Element("p").text("图片未保存：${img.attr("alt")} ")
                if (MarkdownContent.allowsUrl(url, image = true))
                    replacement.appendElement("a").attr("href", url).text("查看来源图片")
                img.replaceWith(replacement)
            }
        }
        doc.select("a[href]").forEach { link ->
            val url = link.attr("href")
            if (!MarkdownContent.allowsUrl(url)) link.unwrap()
            else if (!url.startsWith("#"))
                link.attr("href", resolve(url)).attr("rel", "noreferrer noopener")
        }
        note?.let { doc.body().prependElement("p").text(it) }
        return """<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src data:; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'"><title>c001apk 内容导出</title><style>body{max-width:840px;margin:32px auto;padding:0 20px;font:17px/1.7 system-ui,sans-serif;overflow-wrap:anywhere;color:#202124}img{max-width:100%;height:auto}pre{overflow:auto;background:#f3f4f6;padding:16px}code{font-family:monospace}table{border-collapse:collapse;display:block;overflow:auto}td,th{border:1px solid #ddd;padding:8px}blockquote{border-left:3px solid #aaa;margin-left:0;padding-left:16px}a{color:#1769d2}</style></head><body>${doc.body().html()}</body></html>"""
    }
}
