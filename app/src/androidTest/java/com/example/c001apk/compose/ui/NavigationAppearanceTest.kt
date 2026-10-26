package com.example.c001apk.compose.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.c001apk.compose.logic.datastore.UserPreferencesCompat
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.ui.component.MiuixNavigationScaffold
import com.example.c001apk.compose.ui.component.icons.FlutterProfileIcons
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import top.yukonga.miuix.kmp.basic.Text

class NavigationAppearanceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun navigationAndBadgeSurviveIndependentAppearanceChanges() {
        val preferences = mutableStateOf(UserPreferencesCompat.default())
        val selected = mutableIntStateOf(0)
        compose.setContent {
            CompositionLocalProvider(LocalUserPreferences provides preferences.value) {
                C001apkComposeTheme(materialYou = false) {
                    MiuixNavigationScaffold(
                        items = listOf(
                            FlutterProfileIcons.Comment to "首页",
                            FlutterProfileIcons.PersonAdd to "关注",
                            FlutterProfileIcons.Account to "我的",
                        ),
                        selectedIndex = selected.intValue,
                        wide = false,
                        unreadCount = 3,
                        onSelect = { selected.intValue = it },
                    ) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding)) { Text("页面内容") }
                    }
                }
            }
        }
        for (floating in listOf(false, true, false)) {
            for (blur in listOf(false, true, false)) {
                compose.runOnIdle {
                    preferences.value = preferences.value.copy(
                        floatingNavigationBar = floating,
                        blurBars = blur,
                    )
                }
                (if (floating) compose.onNodeWithContentDescription("关注")
                    else compose.onNodeWithText("关注")).performClick()
                compose.runOnIdle { assertEquals(1, selected.intValue) }
                (if (floating) compose.onNodeWithContentDescription("我的")
                    else compose.onNodeWithText("我的")).performClick()
                compose.runOnIdle { assertEquals(2, selected.intValue) }
                compose.onNodeWithText("3", useUnmergedTree = true).assertIsDisplayed()
                compose.onNodeWithText("页面内容").assertIsDisplayed()
            }
        }
    }
}
