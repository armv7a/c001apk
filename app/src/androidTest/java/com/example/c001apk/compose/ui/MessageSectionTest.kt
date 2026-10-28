package com.example.c001apk.compose.ui

import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.compose.ui.message.MessageSection
import com.example.c001apk.compose.ui.notification.NoticeType
import com.example.c001apk.compose.ui.notification.NotificationCategories
import com.example.c001apk.compose.ui.notification.NotificationPager
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import top.yukonga.miuix.kmp.basic.Text

class MessageSectionTest {
    private val compose = createComposeRule()
    @get:Rule val rule: RuleChain = RuleChain.outerRule(object : ExternalResource() {
        override fun before() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.uiAutomation.executeShellCommand(
                "am start -W -n ${instrumentation.targetContext.packageName}/androidx.activity.ComponentActivity",
            ).use { ParcelFileDescriptor.AutoCloseInputStream(it).use { stream -> stream.readBytes() } }
        }
    }).around(compose)

    @Test fun fourOriginalListRowsRemainVerticalAndOpenTheirOwnDestinations() {
        val opened = mutableListOf<NoticeType>()
        compose.setContent {
            C001apkComposeTheme(materialYou = false, fontScale = 1.3f) {
                MessageSection(
                    modifier = Modifier.width(280.dp),
                    counts = mapOf(NoticeType.AT to 2, NoticeType.COMMENT to 3,
                        NoticeType.NOTIFICATION to 5, NoticeType.LIKE to 107,
                        NoticeType.FOLLOW to 0, NoticeType.MESSAGE to 1),
                    onViewNotice = opened::add,
                )
            }
        }
        compose.onAllNodes(hasClickAction()).assertCountEquals(4)
        compose.onNodeWithText("10").assertIsDisplayed()
        compose.onNodeWithText("99+").assertIsDisplayed()
        compose.onNodeWithText("0").assertDoesNotExist()
        compose.onNodeWithText("@我的动态").assertDoesNotExist()
        compose.onNodeWithText("@我的评论").assertDoesNotExist()
        compose.onNodeWithText("信息").assertDoesNotExist()
        val labels = listOf("通知", "点赞", "关注", "私信")
        val bounds = labels.map { label ->
            compose.onNodeWithText(label).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        }
        bounds.zipWithNext().forEach { (first, second) ->
            assertEquals(first.left, second.left, 1f)
            assertTrue(first.bottom <= second.top)
        }
        labels.forEach { compose.onNodeWithText(it).performClick() }
        compose.runOnIdle {
            assertEquals(listOf(NoticeType.NOTICES, NoticeType.LIKE, NoticeType.FOLLOW, NoticeType.MESSAGE), opened)
        }
    }

    @Test fun notificationTabsSupportSwipesClicksAndRestoreListPositions() {
        lateinit var pager: PagerState
        val lists = mutableMapOf<NoticeType, LazyListState>()
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            C001apkComposeTheme(materialYou = false) {
                val state = rememberPagerState { 3 }
                SideEffect { pager = state }
                Column(Modifier.fillMaxSize()) {
                    NotificationCategories(state)
                    NotificationPager(state, Modifier.weight(1f)) { category ->
                        val list = rememberLazyListState()
                        SideEffect { lists[category] = list }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().testTag("list-${category.name}"),
                            state = list,
                        ) {
                            items(60) { index ->
                                Text(
                                    "${category.name}-$index",
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        fun awaitPage(index: Int, type: NoticeType) {
            compose.waitUntil(5_000) { pager.currentPage == index && !pager.isScrollInProgress }
            compose.onNodeWithTag("list-${type.name}").assertIsDisplayed()
        }

        awaitPage(0, NoticeType.NOTIFICATION)
        compose.onNodeWithTag("list-NOTIFICATION").performScrollToIndex(25)
        compose.onNodeWithTag("list-NOTIFICATION").performTouchInput { swipeLeft() }
        awaitPage(1, NoticeType.AT)
        compose.onNodeWithText("@我的评论").performClick()
        awaitPage(2, NoticeType.COMMENT)
        compose.onNodeWithTag("list-COMMENT").performScrollToIndex(18)
        compose.onNodeWithTag("list-COMMENT").performTouchInput { swipeRight() }
        awaitPage(1, NoticeType.AT)
        compose.onNodeWithText("信息").performClick()
        awaitPage(0, NoticeType.NOTIFICATION)
        compose.runOnIdle { assertEquals(25, lists.getValue(NoticeType.NOTIFICATION).firstVisibleItemIndex) }
        compose.onNodeWithText("@我的评论").performClick()
        awaitPage(2, NoticeType.COMMENT)
        compose.runOnIdle { assertEquals(18, lists.getValue(NoticeType.COMMENT).firstVisibleItemIndex) }

        restoration.emulateSavedInstanceStateRestore()
        awaitPage(2, NoticeType.COMMENT)
        compose.runOnIdle { assertEquals(18, lists.getValue(NoticeType.COMMENT).firstVisibleItemIndex) }
        compose.onNodeWithText("信息").performClick()
        awaitPage(0, NoticeType.NOTIFICATION)
        compose.runOnIdle {
            assertEquals(25, lists.getValue(NoticeType.NOTIFICATION).firstVisibleItemIndex)
        }
    }
}
