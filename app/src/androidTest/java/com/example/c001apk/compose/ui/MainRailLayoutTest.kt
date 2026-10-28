package com.example.c001apk.compose.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.compose.logic.datastore.UserPreferencesCompat
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.ui.component.MiuixNavigationRail
import com.example.c001apk.compose.ui.component.MiuixNavigationScaffold
import com.example.c001apk.compose.ui.component.ReadingSidebarButton
import com.example.c001apk.compose.ui.main.MainNavigationLayout
import com.example.c001apk.compose.ui.main.MainPageState
import com.example.c001apk.compose.ui.main.mainNavigationItems
import com.example.c001apk.compose.ui.main.rememberMainPageState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainRailLayoutTest {
    private val compose = createComposeRule()
    @get:Rule val rule: RuleChain = RuleChain.outerRule(object : ExternalResource() {
        override fun before() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val appId = instrumentation.targetContext.packageName
            instrumentation.uiAutomation.executeShellCommand(
                "am start -W -n $appId/androidx.activity.ComponentActivity",
            ).use { descriptor ->
                android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
            }
        }
    }).around(compose)

    private val split = mutableStateOf(false)
    private val wide = mutableStateOf(true)
    private val badge = mutableIntStateOf(3)
    private lateinit var state: MainPageState

    @Test
    fun collapsingSidebarKeepsBothScrollPositionsAndExpandsReadingWidth() {
        val hidden = mutableStateOf(false)
        lateinit var mainList: LazyListState
        lateinit var readingList: LazyListState
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MiuixTheme {
                    MainNavigationLayout(
                        split = !hidden.value,
                        detailWidth = if (hidden.value) 2000.dp else 450.dp,
                        rail = {},
                        main = { modifier ->
                            mainList = rememberLazyListState()
                            LazyColumn(modifier, state = mainList) {
                                items(80) { Text("动态 $it", Modifier.height(80.dp)) }
                            }
                        },
                        detail = { modifier ->
                            readingList = rememberLazyListState()
                            Column(modifier.testTag("reading")) {
                                ReadingSidebarButton(hidden.value) { hidden.value = !hidden.value }
                                LazyColumn(state = readingList) {
                                    items(80) { Text("正文 $it", Modifier.height(80.dp)) }
                                }
                            }
                        },
                    )
                }
            }
        }
        compose.runOnIdle { mainList.requestScrollToItem(12); readingList.requestScrollToItem(17) }
        compose.waitForIdle()
        val before = compose.onNodeWithTag("reading").fetchSemanticsNode().boundsInRoot.width
        compose.onNodeWithContentDescription("收起信息流").performClick()
        val expanded = compose.onNodeWithTag("reading").fetchSemanticsNode().boundsInRoot.width
        assertTrue(expanded > before)
        compose.onNodeWithContentDescription("显示信息流").performClick()
        compose.runOnIdle {
            assertEquals(12, mainList.firstVisibleItemIndex)
            assertEquals(17, readingList.firstVisibleItemIndex)
        }
    }

    private fun showLayout() {
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f),
                LocalUserPreferences provides UserPreferencesCompat.default(),
            ) {
                MiuixTheme {
                    state = rememberMainPageState()
                    val items = mainNavigationItems(state.selectedIndex)
                    val onSelect: (Int) -> Unit = {
                        state.select(it)
                        if (it == 2) badge.intValue = 0
                    }
                    // Exercise tablet constraints without changing the device's display settings.
                    Box(Modifier.width(if (wide.value) 1000.dp else 500.dp).fillMaxHeight().testTag("root")) {
                        MainNavigationLayout(
                            split = split.value,
                            detailWidth = if (split.value) 520.dp else 0.dp,
                            rail = {
                                if (wide.value) Box(Modifier.testTag("rail")) {
                                    MiuixNavigationRail(items, state.selectedIndex, badge.intValue, onSelect)
                                }
                            },
                            main = { modifier ->
                                Box(modifier.testTag("main")) {
                                    MiuixNavigationScaffold(
                                        items, state.selectedIndex, wide.value, badge.intValue,
                                        onSelect, showRail = false,
                                    ) { padding ->
                                        Box(Modifier.fillMaxSize().padding(padding)) { Text("列表") }
                                    }
                                }
                            },
                            detail = { modifier -> Box(modifier.testTag("detail")) { Text("详情") } },
                        )
                    }
                }
            }
        }
    }

    @Test
    fun railStaysAtLeftWhenDetailOpensAndCloses() {
        showLayout()
        val railBefore = compose.onNodeWithTag("rail").fetchSemanticsNode().boundsInRoot
        val root = compose.onNodeWithTag("root").fetchSemanticsNode().boundsInRoot
        assertEquals(root.left, railBefore.left, 1f)
        assertEquals(railBefore.right, compose.onNodeWithTag("main").fetchSemanticsNode().boundsInRoot.left, 1f)
        compose.runOnIdle { split.value = true }
        val railAfter = compose.onNodeWithTag("rail").fetchSemanticsNode().boundsInRoot
        val detail = compose.onNodeWithTag("detail").fetchSemanticsNode().boundsInRoot
        val main = compose.onNodeWithTag("main").fetchSemanticsNode().boundsInRoot
        assertEquals(railBefore, railAfter)
        assertEquals(railAfter.right, detail.left, 1f)
        assertEquals(detail.right, main.left, 1f)
        assertEquals(root.right, main.right, 1f)
        compose.onNodeWithText("详情").assertIsDisplayed()
        compose.runOnIdle { split.value = false }
        assertEquals(railBefore, compose.onNodeWithTag("rail").fetchSemanticsNode().boundsInRoot)
        assertEquals(railBefore.right, compose.onNodeWithTag("main").fetchSemanticsNode().boundsInRoot.left, 1f)
    }

    @Test
    fun selectionReselectionAndBadgeWorkAcrossWideAndCompactLayouts() {
        showLayout()
        compose.onNodeWithText("关注").performClick()
        compose.runOnIdle {
            assertEquals(1, state.selectedIndex)
            assertFalse(state.followRefreshing)
        }
        compose.onNodeWithText("关注").performClick()
        compose.runOnIdle { assertTrue(state.followRefreshing) }
        compose.onNodeWithText("3", useUnmergedTree = true).assertIsDisplayed()
        compose.runOnIdle { wide.value = false }
        compose.runOnIdle { assertEquals(1, state.selectedIndex) }
        compose.onNodeWithText("我的").performClick()
        compose.runOnIdle {
            assertEquals(2, state.selectedIndex)
            assertEquals(0, badge.intValue)
        }
        compose.runOnIdle { wide.value = true }
        compose.runOnIdle { assertEquals(2, state.selectedIndex) }
        compose.onNodeWithText("主页").performClick()
        compose.onNodeWithText("主页").performClick()
        compose.runOnIdle { assertTrue(state.homeRefreshing) }
    }
}
