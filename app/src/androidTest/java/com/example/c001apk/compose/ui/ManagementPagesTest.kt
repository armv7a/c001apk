package com.example.c001apk.compose.ui

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.compose.logic.follow.LocalFollowUser
import com.example.c001apk.compose.logic.model.StringEntity
import com.example.c001apk.compose.ui.blacklist.BlackListContent
import com.example.c001apk.compose.ui.blacklist.BlackListType
import com.example.c001apk.compose.ui.component.GroupedListItem
import com.example.c001apk.compose.ui.component.MiuixScaffold
import com.example.c001apk.compose.ui.component.MiuixTopBar
import com.example.c001apk.compose.ui.follow.LocalFollowHeader
import com.example.c001apk.compose.ui.follow.LocalFollowUserRow
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

/** Exercise nested actions and retained list identities without account or network writes. */
class ManagementPagesTest {
    private val compose = createComposeRule()
    @get:Rule val rule: RuleChain = RuleChain.outerRule(object : ExternalResource() {
        override fun before() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.uiAutomation.executeShellCommand(
                "am start -W -n ${instrumentation.targetContext.packageName}/androidx.activity.ComponentActivity",
            ).use { ParcelFileDescriptor.AutoCloseInputStream(it).use { stream -> stream.readBytes() } }
        }
    }).around(compose)

    @Test fun addAndRemoveUserDoesNotOpenTheProfile() {
        val entries = mutableStateOf(emptyList<StringEntity>())
        val query = mutableStateOf("")
        val opened = mutableListOf<String>()
        compose.setContent {
            C001apkComposeTheme(materialYou = false) {
                MiuixScaffold(topBar = { MiuixTopBar("用户黑名单") }) { padding ->
                    BlackListContent(BlackListType.USER, entries.value, query.value, padding,
                        onQueryChange = { query.value = it },
                        onAdd = { entries.value += StringEntity(query.value); query.value = "" },
                        onOpen = opened::add,
                        onRemove = { uid -> entries.value = entries.value.filterNot { it.data == uid } })
                }
            }
        }
        compose.onNodeWithText("添加").assertIsNotEnabled()
        compose.onNodeWithText("还没有屏蔽的用户").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextInput("108623")
        compose.onNodeWithText("添加").performClick()
        compose.onNodeWithText("UID 108623").assertIsDisplayed()
        compose.onNodeWithText("添加").assertIsNotEnabled()
        capture("blacklist-user-fixture.png")
        compose.onNodeWithContentDescription("移除用户 UID 108623").performClick()
        compose.onNodeWithText("还没有屏蔽的用户").assertIsDisplayed()
        compose.runOnIdle { assertTrue(opened.isEmpty()) }
    }

    @Test fun topicNamesDoNotCollideWithSectionKeysAndLongTextKeepsActionsVisible() {
        val longName = "这是一个用于检查长话题名称换行和操作按钮位置的话题".repeat(3)
        val entries = mutableStateOf(listOf(StringEntity("add"), StringEntity("hint"), StringEntity(longName)))
        val opened = mutableListOf<String>()
        compose.setContent {
            C001apkComposeTheme(materialYou = false, darkTheme = true, fontScale = 1.3f) {
                MiuixScaffold(topBar = { MiuixTopBar("话题黑名单") }) { padding ->
                    BlackListContent(BlackListType.TOPIC, entries.value, "", padding,
                        onQueryChange = {}, onAdd = {}, onOpen = opened::add,
                        onRemove = { name -> entries.value = entries.value.filterNot { it.data == name } })
                }
            }
        }
        compose.onNodeWithText("hint").performClick()
        compose.runOnIdle { assertEquals(listOf("hint"), opened) }
        val remove = compose.onNodeWithContentDescription("移除话题 $longName")
        remove.performScrollTo().assertIsDisplayed()
        val bounds = remove.fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.left >= root.left && bounds.right <= root.right)
        capture("blacklist-topic-large-dark.png")
        remove.performClick()
        compose.runOnIdle { assertEquals(listOf("add", "hint"), entries.value.map { it.data }) }
    }

    @Test fun localFollowOverflowOnlyRemovesTheChosenUser() {
        var opened = 0
        var removed = 0
        val user = LocalFollowUser("108623", "用于检查长昵称布局的本地关注用户".repeat(3), "")
        compose.setContent {
            C001apkComposeTheme(materialYou = false, fontScale = 1.3f) {
                MiuixScaffold(topBar = { MiuixTopBar("本地关注") }) { padding ->
                    Column(Modifier.fillMaxWidth().padding(padding)) {
                        GroupedListItem(first = true, last = true) {
                            LocalFollowUserRow(user, onUser = { opened++ }, onRemove = { removed++ })
                        }
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("移除本地关注").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, removed); assertEquals(0, opened) }
        compose.onNodeWithText(user.name).performClick()
        compose.runOnIdle { assertEquals(1, opened) }
        capture("local-follow-large-name.png")
    }

    @Test fun localFeedManagementAndRetryAreIndependentActions() {
        var managed = 0
        var retried = 0
        compose.setContent {
            C001apkComposeTheme(materialYou = false) {
                LocalFollowHeader("部分用户加载失败", onManage = { managed++ }, onRetry = { retried++ })
            }
        }
        compose.onNodeWithText("管理").performClick()
        compose.onNodeWithText("重试").performClick()
        compose.runOnIdle { assertEquals(1, managed); assertEquals(1, retried) }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(instrumentation.targetContext.cacheDir, name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
