package com.example.c001apk.compose.ui.topic

import java.net.URLDecoder
import java.net.URLEncoder
import org.junit.Assert.*
import org.junit.Test

class TopicSortingTest {
    private fun wrapped(inner: String) = "/page?url=" + URLEncoder.encode(inner, "UTF-8")
    private fun query(url: String): Map<String, String> = URLDecoder.decode(url.substringAfter("/page?url="), "UTF-8")
        .substringAfter('?').split('&').associate {
            URLDecoder.decode(it.substringBefore('='), "UTF-8") to URLDecoder.decode(it.substringAfter('='), "UTF-8")
        }

    @Test fun eachProductSubtabKeepsItsCategoryAndSummaryWhileSorting() {
        for (subId in 1..6) {
            val original = wrapped("/product/feedList?type=subTabFeed&id=5801&subId=$subId&withSubTabFeedCard=1&withSortCard=1&ignoreEntityById=1")
            assertEquals(listOf("默认", "最新", "热度"), topicSortOptions(original).map { it.label })
            for ((sort, value) in listOf(ProductSortType.DATELINE to "dateline_desc", ProductSortType.HOT to "rank_score")) {
                val result = query(sortedTopicUrl(original,sort))
                assertEquals("subTabFeed",result["type"])
                assertEquals("5801",result["id"])
                assertEquals(subId.toString(),result["subId"])
                assertEquals("1",result["withSubTabFeedCard"])
                assertEquals(value,result["listType"])
            }
            assertEquals(original,sortedTopicUrl(original,ProductSortType.REPLY))
        }
    }

    @Test fun specializedFeedTypesAreNeverReplacedByDiscussion() {
        for (type in listOf("article","answer","video","trade")) {
            val url=wrapped("/product/feedList?type=$type&id=5111&listType=dateline_desc")
            assertEquals(type,query(sortedTopicUrl(url,ProductSortType.HOT))["type"])
            assertEquals("rank_score",query(sortedTopicUrl(url,ProductSortType.HOT))["listType"])
        }
    }

    @Test fun ratingFiltersAreIndependentOfOwnerOnlyMode() {
        val url=wrapped("/product/feedList?type=rating&isOwner=1&withRatingCard=1&id=5111")
        assertEquals(listOf("机主","最新","好评","差评","热度"),topicSortOptions(url).map { it.label })
        assertEquals("1",query(sortedTopicUrl(url,ProductSortType.OWNER))["isOwner"])
        for ((sort,type) in listOf(ProductSortType.DATELINE to "rating",ProductSortType.HOT to "rating",ProductSortType.GOOD to "ratingByScore",ProductSortType.BAD to "ratingByScoreAsc")) {
            val result=query(sortedTopicUrl(url,sort))
            assertEquals(type,result["type"])
            assertFalse(result.containsKey("isOwner"))
            assertEquals("5111",result["id"])
        }
    }

    @Test fun topicTagFeedListUsesSortFieldAndKeepsEncodedTitle() {
        val url="#/topic/tagFeedList?type=feed&id=13192&title=%E6%89%8B%E6%9C%BA%E6%91%84%E5%BD%B1&sortField=lastupdate_desc"
        val hot=query(sortedTopicUrl(url,ProductSortType.HOT))
        assertEquals("rank_score",hot["sortField"])
        assertFalse(hot.containsKey("listType"))
        assertEquals("手机摄影",hot["title"])
        assertEquals("dateline_desc",query(sortedTopicUrl(url,ProductSortType.DATELINE))["sortField"])
    }

    @Test fun multiTagListKeepsTagAndUsesPopular() {
        val url="#/feed/multiTagFeedList?listType=lastupdate_desc&isIncludeTop=1&tag=C%2B%2B"
        val result=query(sortedTopicUrl(url,ProductSortType.HOT))
        assertEquals("C++",result["tag"])
        assertEquals("popular",result["listType"])
        assertFalse(result.containsKey("isIncludeTop"))
    }

    @Test fun mixedEncodedAndRawPageFlagsStayInsideTheRebuiltUrl() {
        val url=wrapped("/product/feedList?type=subTabFeed&id=5801&subId=6")+"&ignoreEntityById=1"
        val result=query(sortedTopicUrl(url,ProductSortType.DATELINE))
        assertEquals("6",result["subId"])
        assertEquals("1",result["ignoreEntityById"])
        assertEquals("dateline_desc",result["listType"])
        val raw="/page?url=/product/feedList?type=article&id=5111"
        assertEquals("article",query(sortedTopicUrl(raw,ProductSortType.DATELINE))["type"])
    }

    @Test fun nonFeedTabsAndMalformedUrlsDoNotAcquireSorting() {
        for (url in listOf("/topic/tagList?keywords=test",wrapped("/product/feedList?type=main&id=5111"),"/page?url=%2Fproduct%2FfeedList%ZZ")) {
            assertTrue(topicSortOptions(url).isEmpty())
            assertEquals(url,sortedTopicUrl(url,ProductSortType.HOT))
        }
    }
}
