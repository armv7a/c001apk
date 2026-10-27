package com.example.c001apk.compose.ui.main

import org.junit.Assert.*
import org.junit.Test

class MainPageStateTest {
    @Test
    fun reselectingHomeAndFollowRequestsOnlyTheirOwnRefresh() {
        val state = MainPageState()
        state.select(0)
        assertTrue(state.homeRefreshing)
        assertFalse(state.followRefreshing)
        state.homeRefreshing = false

        state.select(1)
        assertFalse(state.followRefreshing)
        state.select(1)
        assertTrue(state.followRefreshing)
        assertFalse(state.homeRefreshing)
    }

    @Test
    fun selectionAndBackDoNotRequestUnrelatedRefreshes() {
        val state = MainPageState(2)
        state.select(2)
        state.selectedIndex = 0
        assertEquals(0, state.selectedIndex)
        assertFalse(state.homeRefreshing)
        assertFalse(state.followRefreshing)
    }
}
