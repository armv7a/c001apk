package com.example.c001apk.compose.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.c001apk.compose.ui.component.edgeSwipeBack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

class EdgeSwipeBackTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var navigation: NavHostController
    private lateinit var pager: PagerState
    private lateinit var list: LazyListState
    private val overlay = mutableStateOf(false)

    private fun openDetail() {
        compose.setContent {
            MiuixTheme {
                navigation = rememberNavController()
                val entry by navigation.currentBackStackEntryAsState()
                Box(
                    Modifier.fillMaxSize()
                        .edgeSwipeBack(enabled = entry?.destination?.route == "detail")
                        .testTag("navigation"),
                ) {
                    NavHost(navigation, startDestination = "home") {
                        composable("home") { Text("首页") }
                        composable("detail") {
                            pager = rememberPagerState(initialPage = 1, pageCount = { 3 })
                            list = rememberLazyListState()
                            LazyColumn(Modifier.fillMaxSize().testTag("list"), state = list) {
                                item {
                                    HorizontalPager(
                                        state = pager,
                                        modifier = Modifier.height(180.dp).testTag("pager"),
                                    ) { Text("排序 $it", Modifier.fillMaxSize()) }
                                }
                                items(30) { Text("内容 $it", Modifier.height(100.dp)) }
                            }
                            BackHandler(enabled = overlay.value) { overlay.value = false }
                        }
                    }
                }
            }
        }
        compose.runOnIdle { navigation.navigate("detail") }
        compose.waitForIdle()
    }

    @Test
    fun shortDragCancelsAndLongEdgeDragReturnsOnce() {
        openDetail()
        compose.onNodeWithTag("navigation").performTouchInput {
            swipe(Offset(30f, centerY), Offset(width * .15f, centerY), durationMillis = 800)
        }
        compose.runOnIdle { assertEquals("detail", navigation.currentDestination?.route) }
        compose.onNodeWithTag("navigation").performTouchInput {
            swipe(Offset(30f, centerY), Offset(width * .7f, centerY), durationMillis = 800)
        }
        compose.runOnIdle { assertEquals("home", navigation.currentDestination?.route) }
        // The same gesture at the root must not finish the activity or navigate again.
        compose.onNodeWithTag("navigation").performTouchInput {
            swipe(Offset(30f, centerY), Offset(width * .7f, centerY), durationMillis = 800)
        }
        compose.runOnIdle { assertEquals("home", navigation.currentDestination?.route) }
    }

    @Test
    fun middleDragStillChangesHorizontalPage() {
        openDetail()
        compose.onNodeWithTag("pager").performTouchInput {
            swipe(Offset(width * .35f, centerY), Offset(width * .9f, centerY), durationMillis = 500)
        }
        compose.runOnIdle {
            assertEquals(0, pager.currentPage)
            assertEquals("detail", navigation.currentDestination?.route)
        }
    }

    @Test
    fun verticalEdgeDragStillScrollsContent() {
        openDetail()
        compose.onNodeWithTag("list").performTouchInput {
            swipe(Offset(30f, height * .8f), Offset(30f, height * .3f), durationMillis = 600)
        }
        compose.runOnIdle {
            assertTrue(list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 0)
            assertEquals("detail", navigation.currentDestination?.route)
        }
    }

    @Test
    fun activeBackHandlerClosesOverlayBeforeLeavingPage() {
        openDetail()
        compose.runOnIdle { overlay.value = true }
        compose.onNodeWithTag("navigation").performTouchInput {
            swipe(Offset(30f, centerY), Offset(width * .7f, centerY), durationMillis = 800)
        }
        compose.runOnIdle {
            assertFalse(overlay.value)
            assertEquals("detail", navigation.currentDestination?.route)
        }
    }
}
