package com.example.c001apk.compose.ui

import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.compose.ui.message.MessageSection
import com.example.c001apk.compose.ui.notification.NoticeType
import com.example.c001apk.compose.ui.notification.NotificationCategories
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

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
        val labels = listOf("通知", "赞", "关注", "私信")
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

    @Test fun notificationGroupStillExposesEachOriginalCategory() {
        val opened = mutableListOf<NoticeType>()
        compose.setContent {
            C001apkComposeTheme(materialYou = false) {
                var selected by remember { mutableStateOf(NoticeType.NOTIFICATION) }
                NotificationCategories(selected = selected, onSelected = {
                    selected = it
                    opened += it
                })
            }
        }
        listOf("@我的动态", "@我的评论", "信息").forEach {
            compose.onNodeWithText(it).assertIsDisplayed().performClick()
        }
        compose.runOnIdle {
            assertEquals(listOf(NoticeType.AT, NoticeType.COMMENT, NoticeType.NOTIFICATION), opened)
        }
    }
}
