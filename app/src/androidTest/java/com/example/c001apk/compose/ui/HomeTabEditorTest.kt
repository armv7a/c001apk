package com.example.c001apk.compose.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.compose.logic.database.HomeMenuDatabase
import com.example.c001apk.compose.logic.dao.HomeMenuDao
import androidx.compose.runtime.DisposableEffect
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlinx.coroutines.CompletableDeferred
import com.example.c001apk.compose.logic.model.HomeMenu
import com.example.c001apk.compose.logic.repository.HomeMenuRepo
import com.example.c001apk.compose.ui.home.HomeTabEditorScreen
import com.example.c001apk.compose.ui.home.HomeViewModel
import com.example.c001apk.compose.ui.home.homeTabTypes
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class HomeTabEditorTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun reorderedDraftSurvivesRestorationAndSavesOrderWithVisibilityTogether() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, HomeMenuDatabase::class.java).build()
        try {
            val repo = HomeMenuRepo(db.homeMenuDao())
            runBlocking { repo.upsertList(homeTabTypes.mapIndexed { index, type -> HomeMenu(index, type.name, true) }) }
            val model = HomeViewModel(repo)
            val returned = AtomicBoolean(false)
            val restore = StateRestorationTester(compose)
            restore.setContent { C001apkComposeTheme(materialYou = false) {
                HomeTabEditorScreen(onBackClick = { returned.set(true) }, viewModel = model)
            } }
            compose.waitForIdle()
            compose.onNodeWithText("应用").performClick()
            val first = compose.onAllNodesWithContentDescription("拖动排序", useUnmergedTree = true)[0]
            val a = compose.onNodeWithText("应用").fetchSemanticsNode().boundsInRoot
            val b = compose.onNodeWithText("动态").fetchSemanticsNode().boundsInRoot
            first.performTouchInput { down(center); moveBy(Offset(0f, b.top - a.top), delayMillis = 500); up() }
            compose.waitForIdle()
            assertTrue(compose.onNodeWithText("动态").fetchSemanticsNode().boundsInRoot.top < compose.onNodeWithText("应用").fetchSemanticsNode().boundsInRoot.top)
            restore.emulateSavedInstanceStateRestore()
            compose.waitForIdle()
            assertTrue(compose.onNodeWithText("动态").fetchSemanticsNode().boundsInRoot.top < compose.onNodeWithText("应用").fetchSemanticsNode().boundsInRoot.top)
            compose.onNodeWithText("完成").performClick()
            compose.waitUntil(5000) { returned.get() }
            val saved = runBlocking { repo.loadAllList() }
            assertEquals(listOf("FEED", "APP", "HOT", "TOPIC", "PRODUCT", "COOLPIC"), saved.map { it.title })
            assertFalse(saved.single { it.title == "APP" }.isEnable)
            assertEquals(saved.indices.toList(), saved.map { it.position })
        } finally { db.close() }
    }

    @Test
    fun anAcceptedSaveFinishesBeforeBackCanDiscardTheEntry() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, HomeMenuDatabase::class.java).build()
        val releaseWrite = CompletableDeferred<Unit>()
        try {
            val dao = db.homeMenuDao()
            runBlocking { dao.upsertList(homeTabTypes.mapIndexed { index, type -> HomeMenu(index, type.name, true) }) }
            val waitingDao = object : HomeMenuDao by dao {
                override suspend fun upsertList(list: List<HomeMenu>) {
                    releaseWrite.await()
                    dao.upsertList(list)
                }
            }
            val model = HomeViewModel(HomeMenuRepo(waitingDao))
            val returned = AtomicBoolean(false)
            val input = DirectNavigationEventInput()
            compose.setContent { C001apkComposeTheme(materialYou = false) {
                val dispatcher = checkNotNull(LocalNavigationEventDispatcherOwner.current).navigationEventDispatcher
                DisposableEffect(dispatcher) {
                    dispatcher.addInput(input)
                    onDispose { dispatcher.removeInput(input) }
                }
                HomeTabEditorScreen(onBackClick = { returned.set(true) }, viewModel = model)
            } }
            compose.waitForIdle()
            compose.onNodeWithText("应用").performClick()
            compose.onNodeWithText("完成").performClick()
            compose.runOnIdle { assertTrue(model.saving.value); input.backCompleted() }
            compose.onAllNodesWithContentDescription("返回")[0].performClick()
            compose.runOnIdle { assertFalse(returned.get()) }
            releaseWrite.complete(Unit)
            compose.waitUntil(5000) { returned.get() }
            assertFalse(runBlocking { dao.loadAllList() }.single { it.title == "APP" }.isEnable)
        } finally { releaseWrite.complete(Unit); db.close() }
    }

    @Test
    fun backDiscardsChangesAndLastEnabledTabRemainsDraggable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, HomeMenuDatabase::class.java).build()
        try {
            val repo = HomeMenuRepo(db.homeMenuDao())
            val original = homeTabTypes.mapIndexed { index, type -> HomeMenu(index, type.name, type.name == "APP") }
            runBlocking { repo.upsertList(original) }
            val model = HomeViewModel(repo)
            val returned = AtomicBoolean(false)
            compose.setContent { C001apkComposeTheme(materialYou = false) {
                HomeTabEditorScreen(onBackClick = { returned.set(true) }, viewModel = model)
            } }
            compose.waitForIdle()
            compose.onNodeWithText("应用").performClick()
            val first = compose.onAllNodesWithContentDescription("拖动排序", useUnmergedTree = true)[0]
            val a = compose.onNodeWithText("应用").fetchSemanticsNode().boundsInRoot
            val b = compose.onNodeWithText("动态").fetchSemanticsNode().boundsInRoot
            first.performTouchInput { down(center); moveBy(Offset(0f, b.top - a.top), delayMillis = 500); up() }
            compose.waitForIdle()
            assertTrue(compose.onNodeWithText("动态").fetchSemanticsNode().boundsInRoot.top < compose.onNodeWithText("应用").fetchSemanticsNode().boundsInRoot.top)
            compose.onAllNodesWithContentDescription("返回")[0].performClick()
            compose.runOnIdle { assertTrue(returned.get()) }
            assertEquals(original, runBlocking { repo.loadAllList() })
        } finally { db.close() }
    }
}
