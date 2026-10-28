package com.example.c001apk.compose.ui.main

import com.example.c001apk.compose.logic.model.UpdateCheckItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import top.yukonga.miuix.kmp.nav.core.NavController
import top.yukonga.miuix.kmp.nav.core.navBackStackOf

class AppNavigatorTest {
    private fun navigator(root: AppDestination = AppDestination.Main) =
        AppNavigator(NavController(navBackStackOf(AppNavEntry(root))))

    @Test
    fun allDestinationsRoundTripWithStableVisitIdsAndUnescapedArguments() {
        val special = "中文 / null ?q=a&b=%23 #片段 user_tag:%s%d C++ 100% %20 %"
        val destinations = listOf(
            AppDestination.Main, AppDestination.DetailPlaceholder, AppDestination.Settings(special),
            AppDestination.LocalFollows, AppDestination.Accounts, AppDestination.TabEditor,
            AppDestination.DeviceImport, AppDestination.Params, AppDestination.About, AppDestination.License,
            AppDestination.ContentFilters, AppDestination.ContentRule(null), AppDestination.ContentRule(special),
            AppDestination.TwilightRules,
            AppDestination.Feed("123", true), AppDestination.User("456"),
            AppDestination.Search(null, special, "null"), AppDestination.SearchResult(special, null, "null", special),
            AppDestination.CopyText(special), AppDestination.Topic(special, null),
            AppDestination.WebView("https://example.com/a/b?q=$special", true), AppDestination.App("com.example.test"),
            AppDestination.Login, AppDestination.Carousel(special, special), AppDestination.Event("12"),
            AppDestination.Updates(listOf(UpdateCheckItem("packageName", special))),
            AppDestination.FFFList(null, special, "null", null), AppDestination.Dyh("12", special),
            AppDestination.CoolPic(special), AppDestination.Notice(special), AppDestination.BlackList(special),
            AppDestination.History(special), AppDestination.Chat("1_2", "2", special), AppDestination.Collection("12"),
        )
        val visits = destinations.map { AppNavEntry(it) }
        val restored = Json.decodeFromString<List<AppNavEntry>>(Json.encodeToString(visits))
        assertEquals(visits, restored)
        assertEquals(visits.size, restored.map { it.id }.toSet().size)
    }

    @Test
    fun repeatedVisitsHaveDifferentStateButDoubleTapDoesNotPushTwice() {
        val nav = navigator()
        nav.navigateToFeed("12")
        val firstVisit = nav.currentEntry
        nav.navigateToFeed("12")
        assertEquals(2, nav.controller.backStack.size)
        nav.navigateToUser("34")
        nav.navigateToFeed("12")
        assertEquals(firstVisit.destination, nav.currentDestination)
        assertNotEquals(firstVisit.id, nav.currentEntry.id)
    }

    @Test
    fun popKeepsRootAndAccountResetReplacesItsIdentity() {
        val nav = navigator()
        val originalRoot = nav.currentEntry.id
        assertFalse(nav.popBackStack())
        nav.navigate(AppDestination.Settings("account"))
        nav.navigate(AppDestination.Accounts)
        assertTrue(nav.popBackStack())
        assertEquals(AppDestination.Settings("account"), nav.currentDestination)
        nav.reset()
        assertEquals(AppDestination.Main, nav.currentDestination)
        assertNotEquals(originalRoot, nav.currentEntry.id)
        assertFalse(nav.canGoBack)
    }

    @Test
    fun detailReplacementKeepsItsPlaceholderAndOneBackClosesIt() {
        val nav = navigator(AppDestination.DetailPlaceholder)
        val root = nav.currentEntry
        nav.navigateToFeed("12")
        nav.replace(AppDestination.Feed("34", true))
        assertEquals(2, nav.controller.backStack.size)
        assertEquals(root, nav.controller.backStack.first())
        assertEquals(AppDestination.Feed("34", true), nav.currentDestination)
        assertTrue(nav.popBackStack())
        assertEquals(root, nav.currentEntry)
    }

    @Test
    fun readingHistoryPreservesPreviousVisitAndBoundsOldEntries() {
        val nav = navigator(AppDestination.DetailPlaceholder)
        val root = nav.currentEntry
        nav.navigateReading(AppDestination.Feed("1", false))
        val first = nav.currentEntry
        nav.navigateReading(AppDestination.Feed("2", false))
        nav.popBackStack()
        assertEquals(first, nav.currentEntry)
        for (id in 2..12) nav.navigateReading(AppDestination.Feed(id.toString(), false))
        assertEquals(9, nav.controller.backStack.size)
        assertEquals(root, nav.controller.backStack.first())
        assertEquals(AppDestination.Feed("5", false), (nav.controller.backStack[1] as AppNavEntry).destination)
        nav.navigateReading(AppDestination.Feed("12", false))
        assertEquals(9, nav.controller.backStack.size)
        repeat(8) { assertTrue(nav.popBackStack()) }
        assertEquals(root, nav.currentEntry)
    }
}
