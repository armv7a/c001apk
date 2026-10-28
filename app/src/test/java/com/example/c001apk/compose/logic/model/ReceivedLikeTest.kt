package com.example.c001apk.compose.logic.model

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class ReceivedLikeTest {
    private fun row(json: String) = Gson().fromJson(json, HomeFeedResponse.Data::class.java)

    @Test fun postLikeShowsTheLikerAndOriginalPostSeparately() {
        val notice = row("""{
            "entityType":"feed","id":"123","uid":"author","username":"帖子作者",
            "message":"我的<b>帖子</b>","likeUid":"liker","likeUsername":"点赞人",
            "likeAvatar":"https://example.com/liker.png","likeTime":1700000000,
            "dateline":1600000000,"picArr":["https://example.com/post.png"]
        }""").receivedLike()!!
        assertEquals("liker", notice.userId)
        assertEquals("点赞人", notice.username)
        assertEquals("https://example.com/liker.png", notice.avatar)
        assertEquals(1700000000L, notice.timestamp)
        assertEquals("赞了你的帖子", notice.action)
        assertEquals("帖子作者", notice.author)
        assertEquals("我的帖子", notice.summary)
        assertEquals("https://example.com/post.png", notice.thumbnail)
        assertEquals("/feed/123", notice.url)
        assertEquals("我的<b>帖子</b>", notice.copyText)
    }

    @Test fun replyLikeKeepsItsExactReplyTarget() {
        val notice = row("""{
            "entityType":"feed_reply","id":"456","username":"回复作者","message":"我的回复",
            "url":"/feed/123?rid=456","likeTime":1700000000,
            "likeUserInfo":{"uid":"liker","username":"点赞人","userAvatar":"avatar"}
        }""").receivedLike()!!
        assertEquals("赞了你的回复", notice.action)
        assertEquals("liker", notice.userId)
        assertEquals("回复作者", notice.author)
        assertEquals("/feed/123?rid=456", notice.url)
    }

    @Test fun nestedLikerMetadataAlsoWorksForPosts() {
        val notice = row("""{
            "entityType":"feed","id":"123","url":"/feed/123?from=notification",
            "message_title":"<b>文章标题</b>","message":"很长的正文",
            "likeUserInfo":{"uid":"liker","username":"<b>昵称</b>"}
        }""").receivedLike()!!
        assertEquals("昵称", notice.username)
        assertEquals("文章标题", notice.summary)
        assertEquals("/feed/123?from=notification", notice.url)
    }

    @Test fun ordinaryPostsRepliesAndOtherNotificationsKeepTheirOriginalCards() {
        for (json in listOf(
            """{"entityType":"feed","id":"123","likenum":"8","userAction":{"like":1}}""",
            """{"entityType":"feed_reply","id":"456","uid":"author"}""",
            """{"entityType":"notification","type":"contacts_follow","likeUid":"liker"}""",
        )) assertNull(row(json).receivedLike())
    }

    @Test fun missingLikeDetailsNeverFallBackToThePostAuthorOrPublicationTime() {
        val notice = row("""{
            "entityType":"feed_reply","id":"456","uid":"author","username":"回复作者",
            "userAvatar":"author-avatar","dateline":1600000000,"likeUserInfo":{}
        }""").receivedLike()!!
        assertNull(notice.userId)
        assertEquals("用户", notice.username)
        assertNull(notice.avatar)
        assertNull(notice.timestamp)
        assertNull(notice.url)
        assertEquals("查看回复", notice.summary)
    }

    @Test fun emptyNestedFieldsFallBackToFlatLikerFields() {
        val notice = row("""{
            "entityType":"feed","likeUserInfo":{"uid":"","username":"","userAvatar":""},
            "likeUid":"liker","likeUsername":"点赞人","likeAvatar":"avatar","likeTime":0,
            "pic":"","picArr":["","thumbnail"],"userInfo":{"username":"作者"}
        }""").receivedLike()!!
        assertEquals("liker", notice.userId)
        assertEquals("点赞人", notice.username)
        assertEquals("avatar", notice.avatar)
        assertEquals("作者", notice.author)
        assertEquals("thumbnail", notice.thumbnail)
        assertNull(notice.timestamp)
        assertNull(notice.url)
        assertEquals("查看帖子", notice.summary)
    }
}
