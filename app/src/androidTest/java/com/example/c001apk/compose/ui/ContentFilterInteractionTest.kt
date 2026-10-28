package com.example.c001apk.compose.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.compose.logic.filter.*
import com.example.c001apk.compose.ui.contentfilter.ContentFilterGate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

class ContentFilterInteractionTest {
    private val compose = createComposeRule()
    @get:Rule val rule: RuleChain = RuleChain.outerRule(object : ExternalResource() {
        override fun before() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.uiAutomation.executeShellCommand(
                "am start -W -n ${instrumentation.targetContext.packageName}/androidx.activity.ComponentActivity",
            ).use { descriptor -> android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() } }
        }
    }).around(compose)

    @Test fun revealDoesNotDisableRuleAndDisableRestoresCachedContent() {
        val settings = mutableStateOf(ContentFilterPreferences(enabled = true, rules = listOf(ContentRule("r", "广告", terms = listOf("广告")))))
        val visit = mutableStateOf(0)
        val cached = FilterInput(FilterScope.FEED, "FEED:1", "123", "广告正文")
        compose.setContent {
            MiuixTheme {
                Column {
                    val match = ContentFilterEngine(settings.value).match(cached)
                    key(visit.value) {
                        if (!ContentFilterGate(
                            input = cached, match = match,
                            onDisable = { id -> settings.value = settings.value.copy(rules = settings.value.rules.map { if (it.id == id) it.copy(enabled = false) else it }) },
                            onAllow = { id -> settings.value = settings.value.copy(allowedContent = settings.value.allowedContent + id) },
                        )) Text(cached.text)
                    }
                }
            }
        }
        compose.onNodeWithText("广告正文").assertDoesNotExist()
        compose.onNodeWithText("已过滤：广告").assertIsDisplayed()
        compose.onNodeWithText("仅本次查看").performClick()
        compose.onNodeWithText("广告正文").assertIsDisplayed()
        compose.runOnIdle { assertTrue(settings.value.rules.single().enabled); visit.value++ }
        compose.onNodeWithText("停用此规则").performClick()
        compose.onNodeWithText("广告正文").assertIsDisplayed()
        compose.runOnIdle { assertFalse(settings.value.rules.single().enabled) }
    }
}
