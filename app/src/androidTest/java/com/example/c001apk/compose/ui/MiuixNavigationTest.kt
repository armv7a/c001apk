package com.example.c001apk.compose.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.compose.ui.main.AppDestination
import com.example.c001apk.compose.ui.main.AppNavHost
import com.example.c001apk.compose.ui.main.AppNavigator
import com.example.c001apk.compose.ui.main.rememberAppNavigator
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MiuixNavigationTest {
    private val compose = createComposeRule()
    // HyperOS blocks ActivityScenario's first background launch. Bring the existing test-only
    // activity forward through the instrumentation shell before Compose launches its own instance.
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
    private lateinit var navigation: AppNavigator
    private lateinit var pager: PagerState
    private lateinit var list: LazyListState
    private val overlay = mutableStateOf(false)
    private val back = TestBackInput()

    @Composable
    private fun BackSource() {
        val dispatcher = checkNotNull(LocalNavigationEventDispatcherOwner.current).navigationEventDispatcher
        DisposableEffect(dispatcher) {
            dispatcher.addInput(back)
            onDispose { dispatcher.removeInput(back) }
        }
    }

    private fun openDetail() {
        compose.setContent {
            MiuixTheme {
                BackSource()
                navigation = rememberAppNavigator()
                AppNavHost(navigation, Modifier.fillMaxSize().testTag("navigation")) { entry ->
                    when (entry.destination) {
                        AppDestination.Main -> Text("首页")
                        else -> {
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
        compose.runOnIdle { navigation.navigate(AppDestination.Feed("12")) }
        compose.waitForIdle()
    }

    @Test
    fun shortDragCancelsAndLongDragReturnsOnce() {
        openDetail()
        compose.onNodeWithTag("navigation").performTouchInput {
            swipe(Offset(30f, centerY), Offset(width * .15f, centerY), durationMillis = 800)
        }
        compose.runOnIdle { assertEquals(AppDestination.Feed("12"), navigation.currentDestination) }
        compose.onNodeWithTag("navigation").performTouchInput {
            swipe(Offset(30f, centerY), Offset(width * .7f, centerY), durationMillis = 800)
        }
        compose.runOnIdle { assertEquals(AppDestination.Main, navigation.currentDestination) }
        compose.onNodeWithTag("navigation").performTouchInput {
            swipe(Offset(30f, centerY), Offset(width * .7f, centerY), durationMillis = 800)
        }
        compose.runOnIdle { assertEquals(AppDestination.Main, navigation.currentDestination) }
    }

    @Test
    fun middleDragStillChangesHorizontalPage() {
        openDetail()
        compose.onNodeWithTag("pager").performTouchInput {
            swipe(Offset(width * .35f, centerY), Offset(width * .9f, centerY), durationMillis = 500)
        }
        compose.runOnIdle {
            assertEquals(0, pager.currentPage)
            assertEquals(AppDestination.Feed("12"), navigation.currentDestination)
        }
    }

    @Test
    fun contentAreaUsesNativeSwipeWhenNoChildClaimsMovement() {
        openDetail()
        compose.onNodeWithTag("navigation").performTouchInput {
            swipe(Offset(width * .2f, centerY), Offset(width * .9f, centerY), durationMillis = 800)
        }
        compose.runOnIdle { assertEquals(AppDestination.Main, navigation.currentDestination) }
    }

    @Test
    fun verticalEdgeDragStillScrollsContent() {
        openDetail()
        compose.onNodeWithTag("list").performTouchInput {
            swipe(Offset(30f, height * .8f), Offset(30f, height * .3f), durationMillis = 600)
        }
        compose.runOnIdle {
            assertTrue(list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 0)
            assertEquals(AppDestination.Feed("12"), navigation.currentDestination)
        }
    }

    @Test
    fun systemBackClosesInnerHandlerBeforePoppingTheRoute() {
        openDetail()
        compose.runOnIdle { overlay.value = true }
        compose.runOnIdle { back.complete() }
        compose.runOnIdle {
            assertFalse(overlay.value)
            assertEquals(AppDestination.Feed("12"), navigation.currentDestination)
        }
        compose.runOnIdle { back.complete() }
        compose.runOnIdle { assertEquals(AppDestination.Main, navigation.currentDestination) }
    }

    @Test
    fun webRouteLeavesContentGesturesToThePage() {
        openDetail()
        val web = AppDestination.WebView("https://example.com")
        compose.runOnIdle { navigation.replace(web) }
        compose.onNodeWithTag("navigation").performTouchInput {
            swipe(Offset(30f, centerY), Offset(width * .7f, centerY), durationMillis = 800)
        }
        compose.runOnIdle { assertEquals(web, navigation.currentDestination) }
        compose.runOnIdle { back.complete() }
        compose.runOnIdle { assertEquals(AppDestination.Main, navigation.currentDestination) }
    }

    @Test
    fun inactivePaneCannotConsumeVisiblePagesBack() {
        lateinit var hidden: AppNavigator
        var hiddenBackCalls = 0
        compose.setContent {
            MiuixTheme {
                BackSource()
                navigation = rememberAppNavigator()
                hidden = rememberAppNavigator(AppDestination.DetailPlaceholder)
                Box(Modifier.fillMaxSize()) {
                    AppNavHost(navigation, Modifier.fillMaxSize()) { Text(it.destination.toString()) }
                    AppNavHost(hidden, Modifier.size(0.dp), active = false) {
                        BackHandler { hiddenBackCalls++ }
                    }
                }
            }
        }
        compose.runOnIdle {
            navigation.navigate(AppDestination.User("12"))
            hidden.navigate(AppDestination.Feed("34"))
        }
        compose.runOnIdle { back.complete() }
        compose.runOnIdle {
            assertEquals(AppDestination.Main, navigation.currentDestination)
            assertEquals(AppDestination.Feed("34"), hidden.currentDestination)
            assertEquals(0, hiddenBackCalls)
        }
    }

    @Test
    fun persistedBackStackRestoresVisitIdentityAndNullableArguments() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MiuixTheme {
                navigation = rememberAppNavigator()
                AppNavHost(navigation) { Text(it.destination.toString()) }
            }
        }
        val destination = AppDestination.SearchResult("中文 / ? & #", null, "null", "a/b")
        compose.runOnIdle { navigation.navigate(destination) }
        var visitId = ""
        compose.runOnIdle { visitId = navigation.currentEntry.id }
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            assertEquals(destination, navigation.currentDestination)
            assertEquals(visitId, navigation.currentEntry.id)
            assertTrue(navigation.canGoBack)
        }
    }

    @Test
    fun coveredPageKeepsViewModelAndPoppedPageClearsIt() {
        val models = mutableMapOf<String, ProbeViewModel>()
        compose.setContent {
            MiuixTheme {
                navigation = rememberAppNavigator()
                AppNavHost(navigation) { entry ->
                    models[entry.id] = viewModel()
                    Text(entry.destination.toString())
                }
            }
        }
        var rootId = ""
        lateinit var rootModel: ProbeViewModel
        compose.runOnIdle {
            rootId = navigation.currentEntry.id
            rootModel = checkNotNull(models[rootId])
            navigation.navigate(AppDestination.Feed("12"))
        }
        lateinit var detailModel: ProbeViewModel
        compose.runOnIdle {
            detailModel = checkNotNull(models[navigation.currentEntry.id])
            navigation.navigate(AppDestination.User("34"))
        }
        compose.runOnIdle { navigation.popBackStack() }
        compose.runOnIdle {
            assertSame(detailModel, models[navigation.currentEntry.id])
            assertFalse(detailModel.cleared)
            navigation.popBackStack()
        }
        compose.runOnIdle {
            assertSame(rootModel, models[rootId])
            assertFalse(rootModel.cleared)
            assertTrue(detailModel.cleared)
        }
    }

    class ProbeViewModel : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }

    private class TestBackInput : NavigationEventInput() {
        fun complete() = dispatchOnBackCompleted()
    }
}
