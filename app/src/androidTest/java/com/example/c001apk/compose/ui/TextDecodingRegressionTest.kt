package com.example.c001apk.compose.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.compose.ui.main.AppDestination
import com.example.c001apk.compose.ui.others.CopyTextScreen
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

class TextDecodingRegressionTest {
    private val compose = createComposeRule()

    // Match the navigation test's foreground launch for devices that block background activities.
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

    @Test
    fun copiedPostPreservesPercentSequencesAndPlusAfterRouteRestoration() {
        val text = mutableStateOf("")
        compose.setContent {
            C001apkComposeTheme(materialYou = false) { CopyTextScreen(text.value) }
        }
        for (raw in listOf("user_tag:%s%d", "100%", "%", "%2", "%GG", "C++ + %20 %2B %2520", "中文 👋")) {
            val route: AppDestination = AppDestination.CopyText(raw)
            val restored = Json.decodeFromString<AppDestination>(Json.encodeToString(route))
            compose.runOnIdle { text.value = (restored as AppDestination.CopyText).text.orEmpty() }
            compose.onNodeWithText(raw).assertIsDisplayed()
        }
    }

    @Test
    fun copiedRichTextStillExpandsLinksAndKeepsLineBreaks() {
        val url = "https://%s/gamesafe/mobile/%s/%08X"
        val source = "<b>标签</b>: user_tag:%s%d\n" +
            "<a class=\"feed-link-url\" href=\"$url\" title=\"$url\">查看链接</a>"
        compose.setContent {
            C001apkComposeTheme(materialYou = false) { CopyTextScreen(source) }
        }
        val node = compose.onNodeWithText("标签: user_tag:%s%d", substring = true)
        node.assertIsDisplayed()
        val rendered = node.fetchSemanticsNode().config[SemanticsProperties.Text].single().text
        assertEquals(listOf("标签: user_tag:%s%d", url), rendered.trim().lines().map { it.trim() })
    }
}
