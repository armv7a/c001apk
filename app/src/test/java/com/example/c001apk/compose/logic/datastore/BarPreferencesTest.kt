package com.example.c001apk.compose.logic.datastore

import com.example.c001apk.compose.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BarPreferencesTest {
    @Test
    fun freshAndExistingPreferencesStartWithEffectsOff() {
        val fresh = UserPreferencesCompat.default()
        // An older file has no fields 54–56; proto3 must decode all as false.
        val existing = UserPreferencesCompat(UserPreferences.getDefaultInstance())
        for (preferences in listOf(fresh, existing)) {
            assertFalse(preferences.floatingNavigationBar)
            assertFalse(preferences.blurBars)
            assertFalse(preferences.collapseTabsOnScroll)
        }
    }

    @Test
    fun independentChoicesSurviveSerializationAndUnrelatedChanges() {
        for (floating in listOf(false, true)) {
            for (blur in listOf(false, true)) {
                for (collapseTabs in listOf(false, true)) {
                    val saved = UserPreferencesCompat.default().copy(
                        floatingNavigationBar = floating,
                        blurBars = blur,
                        collapseTabsOnScroll = collapseTabs,
                    )
                    val bytes = saved.copy(contentScale = 0.8f).toProto().toByteArray()
                    val restored = UserPreferencesCompat(UserPreferences.parseFrom(bytes))
                    assertEquals(floating, restored.floatingNavigationBar)
                    assertEquals(blur, restored.blurBars)
                    assertEquals(collapseTabs, restored.collapseTabsOnScroll)
                    assertEquals(0.8f, restored.contentScale)
                }
            }
        }
    }
}
