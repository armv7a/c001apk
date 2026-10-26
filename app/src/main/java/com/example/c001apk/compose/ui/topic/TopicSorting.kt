package com.example.c001apk.compose.ui.topic

import java.net.URLDecoder
import java.net.URLEncoder

enum class ProductSortType { REPLY, HOT, DATELINE, OWNER, GOOD, BAD }

internal data class TopicSortOption(val value: ProductSortType, val label: String)

private data class TopicFeedUrl(val path: String, val parameters: Map<String, String>)

/** Unwrap only the known page container. A few native menu URLs put flags outside its encoded URL. */
private fun parseTopicFeedUrl(original: String): TopicFeedUrl? = runCatching {
    var inner = original.removePrefix("#")
    val outerParameters = linkedMapOf<String, String>()
    if (inner.startsWith("/page?url=")) {
        val raw = inner.removePrefix("/page?url=")
        if (raw.startsWith("%2f", ignoreCase = true)) {
            inner = URLDecoder.decode(raw.substringBefore('&'), "UTF-8")
            if ('&' in raw) outerParameters.putAll(parseParameters(raw.substringAfter('&')))
        } else inner = raw
    }
    val path = inner.substringBefore('?')
    if (path !in setOf("/product/feedList", "/topic/tagFeedList", "/feed/multiTagFeedList")) return null
    TopicFeedUrl(path, parseParameters(inner.substringAfter('?', "")) + outerParameters)
}.getOrNull()

private fun parseParameters(query: String): Map<String, String> = query.split('&')
    .filter { it.isNotEmpty() }
    .associate {
        URLDecoder.decode(it.substringBefore('='), "UTF-8") to
            URLDecoder.decode(it.substringAfter('=', ""), "UTF-8")
    }

internal fun topicSortOptions(url: String): List<TopicSortOption> {
    val target = parseTopicFeedUrl(url) ?: return emptyList()
    if (target.path == "/product/feedList") {
        when (target.parameters["type"]) {
            "rating", "ratingByScore", "ratingByScoreAsc" -> return listOf(
                TopicSortOption(ProductSortType.OWNER, "机主"),
                TopicSortOption(ProductSortType.DATELINE, "最新"),
                TopicSortOption(ProductSortType.GOOD, "好评"),
                TopicSortOption(ProductSortType.BAD, "差评"),
                TopicSortOption(ProductSortType.HOT, "热度"),
            )
            "feed", "subTabFeed", "article", "answer", "video", "trade" -> Unit
            else -> return emptyList()
        }
    }
    return listOf(
        TopicSortOption(ProductSortType.REPLY, "默认"),
        TopicSortOption(ProductSortType.DATELINE, "最新"),
        TopicSortOption(ProductSortType.HOT, "热度"),
    )
}

internal fun sortedTopicUrl(original: String, sort: ProductSortType): String {
    if (sort == ProductSortType.REPLY) return original
    val target = parseTopicFeedUrl(original) ?: return original
    if (topicSortOptions(original).none { it.value == sort }) return original
    val query = target.parameters.toMutableMap()
    query.remove("isIncludeTop")
    when (target.path) {
        "/product/feedList" -> {
            if (query["type"] in setOf("rating", "ratingByScore", "ratingByScoreAsc")) {
                query.remove("isOwner")
                query.remove("listType")
                query["type"] = when (sort) {
                    ProductSortType.GOOD -> "ratingByScore"
                    ProductSortType.BAD -> "ratingByScoreAsc"
                    else -> "rating"
                }
                if (sort == ProductSortType.OWNER) query["isOwner"] = "1"
            }
            if (sort == ProductSortType.DATELINE) query["listType"] = "dateline_desc"
            if (sort == ProductSortType.HOT) {
                query["listType"] = "rank_score"
                query.remove("ignoreEntityById")
            }
        }
        "/topic/tagFeedList" -> {
            query.remove("listType")
            query["sortField"] = if (sort == ProductSortType.HOT) "rank_score" else "dateline_desc"
        }
        "/feed/multiTagFeedList" ->
            query["listType"] = if (sort == ProductSortType.HOT) "popular" else "dateline_desc"
    }
    val inner = target.path + "?" + query.entries.joinToString("&") {
        URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
    }
    return "/page?url=" + URLEncoder.encode(inner, "UTF-8")
}
