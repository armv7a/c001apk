package com.example.c001apk.compose.ui

import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.ui.component.FeedListEntry
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

class ReceivedLikeCardTest {
    private val compose = createComposeRule()
    @get:Rule val rule: RuleChain = RuleChain.outerRule(object : ExternalResource() {
        override fun before() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.uiAutomation.executeShellCommand(
                "am start -W -n ${instrumentation.targetContext.packageName}/androidx.activity.ComponentActivity",
            ).use { ParcelFileDescriptor.AutoCloseInputStream(it).use { stream -> stream.readBytes() } }
        }
    }).around(compose)

    @Test fun postAndReplyLikesUseTheSameCompactCardAndKeepTheirDestinations() {
        val rows = listOf(
            """{"entityType":"feed","id":"123","username":"帖子作者","message":"帖子摘要",
                "likeUid":"liker1","likeUsername":"点赞人一","likeTime":1700000000,"likenum":"999"}""",
            """{"entityType":"feed_reply","id":"456","username":"回复作者","message":"回复摘要",
                "url":"/feed/123?rid=456","likeUserInfo":{"uid":"liker2","username":"点赞人二"},
                "likeTime":1700000000,"likenum":"999"}""",
        ).map { Gson().fromJson(it, HomeFeedResponse.Data::class.java) }
        val users = mutableListOf<String>()
        val links = mutableListOf<String>()
        val copied = mutableListOf<String?>()
        compose.setContent {
            C001apkComposeTheme(materialYou = false, fontScale = 1.3f) {
                Column(Modifier.width(280.dp)) {
                    rows.forEachIndexed { index, row ->
                        FeedListEntry(
                            item = row,
                            index = index,
                            isReceivedLikeList = true,
                            onViewUser = users::add,
                            onViewFeed = { _, _ -> error("Like entries must open the compact preview target") },
                            onOpenLink = { url, _ -> links += url },
                            onCopyText = copied::add,
                            onBlockUser = { _, _ -> },
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("赞了你的帖子").assertIsDisplayed()
        compose.onNodeWithText("赞了你的回复").assertIsDisplayed()
        compose.onNodeWithText("999", substring = true).assertDoesNotExist()
        val previews = compose.onAllNodesWithContentDescription("被赞内容")
        previews.assertCountEquals(2)
        val first = previews[0].fetchSemanticsNode().boundsInRoot
        val second = previews[1].fetchSemanticsNode().boundsInRoot
        assertEquals(first.width, second.width, 1f)
        assertEquals(first.height, second.height, 1f)
        assertTrue(first.right <= compose.onRoot().fetchSemanticsNode().boundsInRoot.right)
        compose.onNodeWithContentDescription("点赞人：点赞人一").performClick()
        compose.onNodeWithContentDescription("点赞人：点赞人二").performClick()
        previews[0].performClick()
        previews[1].performClick()
        previews[0].performTouchInput { longClick() }
        compose.runOnIdle {
            assertEquals(listOf("liker1", "liker2"), users)
            assertEquals(listOf("/feed/123", "/feed/123?rid=456"), links)
            assertEquals(listOf("帖子摘要"), copied)
        }
    }

    @Test fun otherListsKeepTheFullPostEvenWhenLikeMetadataIsPresent() {
        val row = Gson().fromJson("""{
            "entityType":"feed","id":"123","uid":"author","username":"原帖作者",
            "message":"正文","likeUid":"liker","likeUsername":"点赞人","likenum":"999"
        }""", HomeFeedResponse.Data::class.java)
        compose.setContent {
            C001apkComposeTheme(materialYou = false) {
                FeedListEntry(
                    item = row,
                    index = 0,
                    onViewUser = {},
                    onViewFeed = { _, _ -> },
                    onOpenLink = { _, _ -> },
                    onCopyText = {},
                    onBlockUser = { _, _ -> },
                )
            }
        }
        compose.onNodeWithText("原帖作者").assertIsDisplayed()
        compose.onNodeWithText("999", substring = true).assertIsDisplayed()
        compose.onNodeWithText("赞了你的帖子").assertDoesNotExist()
        compose.onNodeWithContentDescription("被赞内容").assertDoesNotExist()
    }
}
