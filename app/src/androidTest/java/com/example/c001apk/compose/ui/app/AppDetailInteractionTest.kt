package com.example.c001apk.compose.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.CollapsingToolbarState
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.PullToRefreshState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme

class AppDetailInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun pullAtListTopExpandsAppInfoBeforeRefreshing() {
        lateinit var header: CollapsingToolbarState
        lateinit var refresh: PullToRefreshState
        val refreshing = mutableStateOf(false)
        var refreshCount = 0
        compose.setContent {
            MiuixTheme {
                val scaffold = rememberCollapsingToolbarScaffoldState()
                header = scaffold.toolbarState
                refresh = rememberPullToRefreshState()
                val connection = rememberAppHeaderScrollConnection(header)
                CollapsingToolbarScaffold(
                    modifier = Modifier.fillMaxSize(),
                    state = scaffold,
                    scrollStrategy = ScrollStrategy.ExitUntilCollapsed,
                    toolbar = {
                        Box(Modifier.fillMaxWidth().height(56.dp))
                        Box(Modifier.fillMaxWidth().height(256.dp))
                    },
                ) {
                    PullToRefresh(
                        isRefreshing = refreshing.value,
                        onRefresh = {
                            refreshCount++
                            refreshing.value = true
                        },
                        pullToRefreshState = refresh,
                    ) {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(1),
                            modifier =
                                Modifier.fillMaxSize().nestedScroll(connection).testTag("feed"),
                        ) {
                            items(30) { Text("评论 $it", Modifier.height(100.dp)) }
                        }
                    }
                }
            }
        }
        compose.runOnIdle { header.dispatchRawDelta(-10_000f) }
        compose.onNodeWithTag("feed").performTouchInput {
            swipe(Offset(centerX, 150f), Offset(centerX, 230f), durationMillis = 1_000)
        }
        compose.runOnIdle {
            assertTrue("App info must expand at the top of the list", header.height > header.minHeight)
            assertEquals("Refresh must wait for the header", 0f, refresh.dragOffset, 0.01f)
            assertEquals(0, refreshCount)
        }
        // A full pull must finish expanding the header, then reach the normal refresh threshold.
        compose.onNodeWithTag("feed").performTouchInput {
            swipe(Offset(centerX, 100f), Offset(centerX, height - 100f), durationMillis = 1_000)
        }
        compose.waitUntil(timeoutMillis = 5_000) { refreshCount == 1 }
        compose.runOnIdle { assertEquals(header.maxHeight, header.height) }
        compose.runOnIdle { refreshing.value = false }
    }

    @Test
    fun textOptionsKeepSelectionAndReselectCallback() {
        val selected = mutableIntStateOf(0)
        var selections = 0
        compose.setContent {
            MiuixTheme {
                AppSortOptions(
                    options = listOf("最近回复", "最新发布", "热度排序"),
                    selectedIndex = selected.intValue,
                    onSelect = {
                        selected.intValue = it
                        selections++
                    },
                )
            }
        }
        compose.onNodeWithText("最近回复").assertIsSelected()
        compose.onNodeWithText("最新发布").performClick().assertIsSelected()
        compose.onNodeWithText("热度排序").performClick().assertIsSelected()
        compose.onNodeWithText("热度排序").performClick()
        compose.runOnIdle { assertEquals(3, selections) }
    }
}
