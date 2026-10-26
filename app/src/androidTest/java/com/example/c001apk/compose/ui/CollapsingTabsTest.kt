package com.example.c001apk.compose.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.logic.datastore.UserPreferencesCompat
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.ui.component.MiuixScaffold
import com.example.c001apk.compose.ui.component.MiuixTabRow
import org.junit.Rule
import org.junit.Test
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

class CollapsingTabsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun scrollCollapsesOnlyWhenEnabledAndDisablingRestoresTabs() {
        val collapse = mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(
                LocalUserPreferences provides
                    UserPreferencesCompat.default().copy(collapseTabsOnScroll = collapse.value),
            ) {
                MiuixTheme {
                    val list = rememberLazyListState()
                    MiuixScaffold(
                        topBar = {
                            Column {
                                Text("固定标题")
                                MiuixTabRow(listOf("动态", "话题"), 0, {})
                            }
                        },
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    ) { padding ->
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().testTag("feed"),
                            state = list,
                            contentPadding = padding,
                        ) {
                            items(50) { Text("内容 $it", Modifier.height(100.dp)) }
                        }
                    }
                }
            }
        }
        fun swipeUp() = compose.onNodeWithTag("feed").performTouchInput {
            swipe(Offset(centerX, height * .8f), Offset(centerX, height * .3f), 600)
        }
        swipeUp()
        compose.onNodeWithText("动态").assertIsDisplayed()
        compose.runOnIdle { collapse.value = true }
        swipeUp()
        compose.onNodeWithText("动态").assertDoesNotExist()
        compose.onNodeWithText("固定标题").assertIsDisplayed()
        compose.onNodeWithTag("feed").performTouchInput {
            swipe(Offset(centerX, height * .3f), Offset(centerX, height * .7f), 600)
        }
        compose.onNodeWithText("动态").assertIsDisplayed()
        swipeUp()
        compose.onNodeWithText("动态").assertDoesNotExist()
        compose.runOnIdle { collapse.value = false }
        compose.onNodeWithText("动态").assertIsDisplayed()
    }
}
