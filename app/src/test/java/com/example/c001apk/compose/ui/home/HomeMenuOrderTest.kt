package com.example.c001apk.compose.ui.home

import com.example.c001apk.compose.logic.model.HomeMenu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeMenuOrderTest {

    @Test
    fun storedOrderAndFlagsAreKeptWhileUnknownAndFollowRowsAreDropped() {
        val stored =
            listOf(
                HomeMenu(position = 3, title = TabType.TOPIC.name, isEnable = false),
                HomeMenu(position = 0, title = TabType.HOT.name, isEnable = true),
                HomeMenu(position = 2, title = TabType.APP.name, isEnable = false),
                HomeMenu(position = 1, title = TabType.FEED.name, isEnable = true),
                HomeMenu(position = 4, title = TabType.FOLLOW.name, isEnable = true),
                HomeMenu(position = 5, title = "REMOVED_TAB", isEnable = true),
            )

        val result = normalizeHomeMenus(stored)

        assertEquals(
            listOf("HOT", "FEED", "APP", "TOPIC", "PRODUCT", "COOLPIC"),
            result.map(HomeMenu::title),
        )
        assertEquals(listOf(0, 1, 2, 3, 4, 5), result.map(HomeMenu::position))
        assertFalse(result.first { it.title == "APP" }.isEnable)
        assertFalse(result.first { it.title == "TOPIC" }.isEnable)
        assertTrue(result.last().isEnable)
        assertFalse(result.any { it.title == TabType.FOLLOW.name })
    }

    @Test
    fun duplicateRowsKeepTheFirstOccurrence() {
        val stored =
            listOf(
                HomeMenu(position = 0, title = TabType.FEED.name, isEnable = false),
                HomeMenu(position = 1, title = TabType.FEED.name, isEnable = true),
                HomeMenu(position = 2, title = TabType.APP.name, isEnable = true),
            )

        val result = normalizeHomeMenus(stored)

        assertEquals(6, result.size)
        assertEquals(TabType.FEED.name, result.first().title)
        assertFalse(result.first().isEnable)
    }

    @Test
    fun emptyStorageAppendsEveryKnownTabEnabled() {
        val result = normalizeHomeMenus(emptyList())

        assertEquals(homeTabTypes.map(TabType::name), result.map(HomeMenu::title))
        assertEquals(homeTabTypes.indices.toList(), result.map(HomeMenu::position))
        assertTrue(result.all(HomeMenu::isEnable))
    }

    @Test
    fun normalizationIsIdempotentAndInMemoryOnly() {
        val once =
            normalizeHomeMenus(
                listOf(
                    HomeMenu(position = 9, title = TabType.FOLLOW.name, isEnable = true),
                    HomeMenu(position = 0, title = TabType.PRODUCT.name, isEnable = false),
                ),
            )

        assertEquals(once, normalizeHomeMenus(once))
        assertEquals(TabType.PRODUCT.name, once.first().title)
        assertFalse(once.first().isEnable)
    }

    @Test
    fun savingKeepsTheDraftOrderWhenPositionsAreStale() {
        // After a drag the draft list is the new order, but every row still carries its old
        // position. Saving must not sort by those stale values.
        val draft =
            listOf(
                HomeMenu(position = 5, title = TabType.PRODUCT.name, isEnable = true),
                HomeMenu(position = 0, title = TabType.FEED.name, isEnable = true),
                HomeMenu(position = 3, title = TabType.HOT.name, isEnable = false),
                HomeMenu(position = 4, title = TabType.APP.name, isEnable = true),
                HomeMenu(position = 1, title = TabType.COOLPIC.name, isEnable = true),
                HomeMenu(position = 2, title = TabType.TOPIC.name, isEnable = true),
            )

        val saved = sanitizeHomeMenus(draft)

        assertEquals(
            listOf("PRODUCT", "FEED", "HOT", "APP", "COOLPIC", "TOPIC"),
            saved.map(HomeMenu::title),
        )
        assertEquals(listOf(0, 1, 2, 3, 4, 5), saved.map(HomeMenu::position))
        assertFalse(saved.first { it.title == "HOT" }.isEnable)
    }

    @Test
    fun savingKeepsOrderAndStillLeavesOneTabEnabled() {
        val draft =
            homeTabTypes.mapIndexed { index, type ->
                HomeMenu(position = index, title = type.name, isEnable = false)
            }

        val saved = sanitizeHomeMenus(draft)

        assertEquals(homeTabTypes.map(TabType::name), saved.map(HomeMenu::title))
        assertEquals(1, saved.count(HomeMenu::isEnable))
        assertEquals(TabType.APP.name, saved.first { it.isEnable }.title)
        assertEquals(saved.indices.toList(), saved.map(HomeMenu::position))
    }

    @Test
    fun savingAnEnabledDraftKeepsFlagsAndAppendsMissingTabsEnabled() {
        val draft =
            listOf(
                HomeMenu(position = 0, title = TabType.HOT.name, isEnable = false),
                HomeMenu(position = 1, title = TabType.APP.name, isEnable = true),
            )

        val saved = sanitizeHomeMenus(draft)

        assertEquals(
            listOf("HOT", "APP", "FEED", "TOPIC", "PRODUCT", "COOLPIC"),
            saved.map(HomeMenu::title),
        )
        assertFalse(saved.first { it.title == "HOT" }.isEnable)
        assertTrue(saved.first { it.title == "APP" }.isEnable)
        assertTrue(saved.first { it.title == "FEED" }.isEnable)
    }
}
