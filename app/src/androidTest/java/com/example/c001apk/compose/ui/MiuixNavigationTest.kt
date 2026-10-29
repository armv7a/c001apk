package com.example.c001apk.compose.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.onSizeChanged
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
import com.example.c001apk.compose.ui.main.AppNavEntry
import com.example.c001apk.compose.ui.main.AppNavigator
import com.example.c001apk.compose.ui.main.MainPageState
import com.example.c001apk.compose.ui.main.MainPageBackHandler
import com.example.c001apk.compose.ui.main.rememberMainPageState
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

    @Test
    fun readingBackStackRestoresScrollPositionAcrossNextArticleAndRecreation() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MiuixTheme {
                navigation = rememberAppNavigator(AppDestination.DetailPlaceholder)
                AppNavHost(navigation, Modifier.fillMaxSize()) { entry ->
                    if (entry.destination is AppDestination.Feed) {
                        val readingList = rememberLazyListState()
                        if (navigation.currentEntry.id == entry.id) list = readingList
                        LazyColumn(state = readingList) {
                            items(80) { Text("段落 $it", modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
        compose.runOnIdle { navigation.navigateReading(AppDestination.Feed("first", false)) }
        compose.waitForIdle()
        compose.runOnIdle { list.requestScrollToItem(18, 11) }
        compose.waitForIdle()
        val firstVisit = navigation.currentEntry.id
        compose.runOnIdle { navigation.navigateReading(AppDestination.Feed("second", false)) }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0, list.firstVisibleItemIndex); navigation.popBackStack() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(firstVisit, navigation.currentEntry.id); assertEquals(18, list.firstVisibleItemIndex) }
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(firstVisit, navigation.currentEntry.id); assertEquals(18, list.firstVisibleItemIndex) }
    }
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
    fun detailBackClosesDetailBeforeReturningFollowListHome() {
        lateinit var detail: AppNavigator
        lateinit var pageState: MainPageState
        compose.setContent {
            MiuixTheme {
                BackSource()
                navigation = rememberAppNavigator()
                detail = rememberAppNavigator(AppDestination.DetailPlaceholder)
                pageState = rememberMainPageState()
                MainPageBackHandler(navigation, pageState, detail.canGoBack) {
                    pageState.select(it, detail)
                }
                Row(Modifier.fillMaxSize()) {
                    AppNavHost(navigation, Modifier.weight(1f).testTag("main-pane")) {
                        Text("列表")
                    }
                    AppNavHost(detail, Modifier.weight(1f), active = detail.canGoBack) {
                        Text(it.destination.toString())
                    }
                }
            }
        }
        compose.runOnIdle {
            pageState.select(1, detail)
            detail.navigateReading(AppDestination.Feed("12"))
        }
        compose.onNodeWithTag("main-pane").performTouchInput {
            down(center)
            up()
        }
        compose.runOnIdle { back.complete() }
        compose.runOnIdle {
            assertEquals(1, pageState.selectedIndex)
            assertFalse(detail.canGoBack)
            assertEquals(AppDestination.Main, navigation.currentDestination)
        }
        compose.runOnIdle { back.complete() }
        compose.runOnIdle { assertEquals(0, pageState.selectedIndex) }
    }

    @Test
    fun closingHiddenDetailKeepsNavigationRootsAliveAndAllowsReopening() {
        lateinit var detail: AppNavigator
        lateinit var pageState: MainPageState
        lateinit var listModel: ProbeViewModel
        val detailModels = mutableMapOf<String, ProbeViewModel>()
        val visible = mutableStateOf(true)
        var contentWidth = 0
        compose.setContent {
            MiuixTheme {
                navigation = rememberAppNavigator()
                detail = rememberAppNavigator(AppDestination.DetailPlaceholder)
                pageState = rememberMainPageState()
                Box(Modifier.fillMaxSize()) {
                    AppNavHost(navigation) {
                        listModel = viewModel()
                        Text("列表")
                    }
                    AppNavHost(
                        detail,
                        Modifier.width(if (visible.value) 400.dp else 0.dp),
                        active = visible.value,
                    ) { entry ->
                        if (entry.destination is AppDestination.Feed) {
                            detailModels[entry.id] = viewModel()
                            LazyColumn(Modifier.onSizeChanged { contentWidth = it.width }) {
                                item {
                                    Column(Modifier.padding(16.dp)) {
                                        Text("详情正文".repeat(100), Modifier.padding(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        var rootId = ""
        var listId = ""
        lateinit var originalListModel: ProbeViewModel
        compose.runOnIdle {
            rootId = (detail.controller.backStack.first() as AppNavEntry).id
            listId = navigation.currentEntry.id
            originalListModel = listModel
            pageState.select(1, detail)
            detail.navigateReading(AppDestination.Feed("12"))
        }
        compose.runOnIdle { detail.navigateReading(AppDestination.Feed("34")) }
        compose.runOnIdle {
            pageState.select(1, detail)
            assertEquals(3, detail.controller.backStack.size)
        }
        compose.runOnIdle { visible.value = false }
        compose.runOnIdle {
            assertTrue("Hidden detail content must not be measured at zero width", contentWidth > 0)
            pageState.select(0, detail)
        }
        compose.runOnIdle {
            assertEquals(AppDestination.Main, navigation.currentDestination)
            assertEquals(listId, navigation.currentEntry.id)
            assertSame(originalListModel, listModel)
            assertFalse(listModel.cleared)
            assertEquals(AppDestination.DetailPlaceholder, detail.currentDestination)
            assertEquals(rootId, detail.currentEntry.id)
            assertFalse(detail.canGoBack)
            assertEquals(2, detailModels.size)
            detailModels.values.forEach { assertTrue(it.cleared) }
            detail.navigateReading(AppDestination.Feed("56"))
            visible.value = true
        }
        compose.runOnIdle {
            assertEquals(AppDestination.Feed("56"), detail.currentDestination)
            assertEquals(2, detail.controller.backStack.size)
            assertEquals(AppDestination.Main, navigation.currentDestination)
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
