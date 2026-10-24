package com.example.c001apk.compose.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.c001apk.compose.logic.markdown.MarkdownContent
import com.example.c001apk.compose.util.ImageShowUtil
import kotlinx.coroutines.launch
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.tables.*
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.node.*
import org.commonmark.node.Text as MdText
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalMarkdownRaw = staticCompositionLocalOf { false }

@Composable
fun RichPostText(
    text: String?,
    modifier: Modifier = Modifier,
    textSize: Float = 16f,
    lineSpacingMultiplier: Float = 1.3f,
    preview: Boolean = false,
    explicit: Boolean = false,
    coolapk: Boolean = true,
    onOpenLink: ((String, String?) -> Unit)? = null,
) {
    val source = text.orEmpty()
    val content =
        remember(source, explicit, coolapk) { MarkdownContent.parse(source, explicit, coolapk) }
    if (LocalMarkdownRaw.current) {
        SelectionContainer(modifier) { Text(source, fontSize = textSize.sp) }
    } else if (!content.isMarkdown) {
        LinkText(
            text = if (coolapk) source.replace("<!--break-->", "") else source,
            modifier = modifier,
            textSize = textSize,
            lineSpacingMultiplier = lineSpacingMultiplier,
            onOpenLink = onOpenLink,
        )
    } else if (preview) {
        Text(
            content.preview,
            modifier = modifier,
            fontSize = textSize.sp,
            lineHeight = (textSize * lineSpacingMultiplier).sp,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
        )
    } else {
        MarkdownBody(content, modifier, textSize, onOpenLink)
    }
}

@Composable
fun MarkdownBody(
    content: MarkdownContent,
    modifier: Modifier = Modifier,
    textSize: Float = 16f,
    onOpenLink: ((String, String?) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val requesters =
        remember(content) { content.headings.values.associateWith { BringIntoViewRequester() } }
    val open: (String) -> Unit = { url ->
        if (url.startsWith("#")) {
            val anchor = url.removePrefix("#")
            val encoded =
                java.net.URLEncoder.encode(java.net.URLDecoder.decode(anchor, "UTF-8"), "UTF-8")
                    .replace("+", "%20")
            requesters[encoded]?.let { scope.launch { it.bringIntoView() } }
        } else if (MarkdownContent.allowsUrl(url)) {
            val resolved =
                if (java.net.URI(url).scheme == null)
                    java.net.URI("https://www.coolapk.com/").resolve(url).toString()
                else url
            onOpenLink?.invoke(resolved, null)
        }
    }
    SelectionContainer(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            content.document?.let { document ->
                MarkdownContent.children(document).forEach {
                    Block(it, content, requesters, textSize, open)
                }
            } ?: Text(content.source)
        }
    }
}

@Composable
private fun InlineText(
    node: Node,
    size: Float,
    open: (String) -> Unit,
    modifier: Modifier = Modifier,
    bold: Boolean = false,
) {
    val primary = MiuixTheme.colorScheme.primary
    val surface = MiuixTheme.colorScheme.surfaceContainer
    val value =
        remember(node, primary, surface, open) {
            buildAnnotatedString {
                fun appendNode(n: Node) {
                    fun children() {
                        MarkdownContent.children(n).forEach { appendNode(it) }
                    }
                    when (n) {
                        is MdText -> append(n.literal)
                        is SoftLineBreak,
                        is HardLineBreak -> append("\n")
                        is Code ->
                            withStyle(
                                SpanStyle(fontFamily = FontFamily.Monospace, background = surface),
                            ) {
                                append(n.literal)
                            }
                        is HtmlInline -> append(n.literal)
                        is StrongEmphasis ->
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { children() }
                        is Emphasis ->
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { children() }
                        is Strikethrough ->
                            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                                children()
                            }
                        is Link ->
                            if (n.destination.isNotBlank())
                                withLink(
                                    LinkAnnotation.Clickable(
                                        n.destination,
                                        TextLinkStyles(style = SpanStyle(color = primary)),
                                    ) {
                                        open(n.destination)
                                    },
                                ) {
                                    children()
                                }
                            else children()
                        is Image ->
                            if (n.destination.isEmpty()) append(MarkdownContent.plainText(n))
                        is TaskListItemMarker -> append(if (n.isChecked) "☑ " else "☐ ")
                        else -> children()
                    }
                }
                appendNode(node)
            }
        }
    Text(
        value,
        modifier = modifier,
        fontSize = size.sp,
        lineHeight = (size * 1.45f).sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    )
}

@Composable
private fun Block(
    node: Node,
    content: MarkdownContent,
    requesters: Map<String, BringIntoViewRequester>,
    size: Float,
    open: (String) -> Unit,
) {
    val surface = MiuixTheme.colorScheme.surfaceContainer
    when (node) {
        is Heading ->
            InlineText(
                node,
                size + (7 - node.level) * 1.7f,
                open,
                Modifier.then(
                    content.headings[node]
                        ?.let { requesters[it] }
                        ?.let { Modifier.bringIntoViewRequester(it) } ?: Modifier,
                ),
                bold = true,
            )
        is org.commonmark.node.Paragraph -> {
            InlineText(node, size, open)
            val images =
                remember(node) {
                    buildList<Image> {
                        fun visit(n: Node) {
                            if (n is Image && n.destination.isNotBlank()) add(n)
                            MarkdownContent.children(n).forEach { visit(it) }
                        }
                        visit(node)
                    }
                }
            val context = LocalContext.current
            images.forEach { img ->
                AsyncImage(
                    model = img.destination,
                    contentDescription = MarkdownContent.plainText(img),
                    modifier =
                        Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 420.dp).clickable {
                            ImageShowUtil.startBigImgViewSimple(context, listOf(img.destination))
                        },
                )
            }
        }
        is FencedCodeBlock,
        is IndentedCodeBlock -> {
            val literal =
                if (node is FencedCodeBlock) node.literal else (node as IndentedCodeBlock).literal
            Column(Modifier.fillMaxWidth().background(surface).padding(12.dp)) {
                if (node is FencedCodeBlock && node.info.isNotBlank())
                    Text(
                        node.info,
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                Text(
                    literal.trimEnd('\n'),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    fontSize = (size - 1).sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
        is BlockQuote ->
            Row(Modifier.fillMaxWidth().background(surface).padding(10.dp)) {
                Box(Modifier.width(3.dp).height(24.dp).background(MiuixTheme.colorScheme.primary))
                Column(
                    Modifier.padding(start = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MarkdownContent.children(node).forEach {
                        Block(it, content, requesters, size, open)
                    }
                }
            }
        is BulletList,
        is OrderedList ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MarkdownContent.children(node).forEachIndexed { index, item ->
                    val task =
                        MarkdownContent.children(item).any { child ->
                            child is TaskListItemMarker ||
                                MarkdownContent.children(child).any { it is TaskListItemMarker }
                        }
                    Row {
                        if (task)
                            Text(
                                if ((item.firstChild as? TaskListItemMarker)?.isChecked == true) "☑"
                                else "☐",
                                modifier = Modifier.width(30.dp),
                                fontSize = size.sp,
                            )
                        if (!task)
                            Text(
                                if (node is OrderedList) "${(node.markerStartNumber ?: 1) + index}."
                                else "•",
                                modifier = Modifier.width(30.dp),
                                fontSize = size.sp,
                            )
                        Column(
                            Modifier.weight(1f).padding(start = if (task) 0.dp else 2.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            MarkdownContent.children(item)
                                .filterNot { it is TaskListItemMarker }
                                .forEach { Block(it, content, requesters, size, open) }
                        }
                    }
                }
            }
        is TableBlock ->
            Column(Modifier.horizontalScroll(rememberScrollState())) {
                MarkdownContent.children(node).forEach { section ->
                    MarkdownContent.children(section).forEach { row ->
                        Row(
                            Modifier.background(
                                if (section is TableHead) surface else Color.Transparent,
                            ),
                        ) {
                            MarkdownContent.children(row).forEach { cell ->
                                InlineText(
                                    cell,
                                    size,
                                    open,
                                    Modifier.width(180.dp).padding(10.dp),
                                    bold = section is TableHead,
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        is ThematicBreak -> HorizontalDivider(Modifier.padding(vertical = 8.dp))
        is HtmlBlock -> Text(node.literal, fontSize = size.sp)
        is TaskListItemMarker -> Text(if (node.isChecked) "☑" else "☐", fontSize = size.sp)
        else ->
            MarkdownContent.children(node).forEach { Block(it, content, requesters, size, open) }
    }
}
