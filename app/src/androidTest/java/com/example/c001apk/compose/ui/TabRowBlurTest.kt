package com.example.c001apk.compose.ui

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.compose.ui.component.MiuixScaffold
import com.example.c001apk.compose.ui.component.MiuixTabRow
import com.example.c001apk.compose.ui.component.MiuixTopBar
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

@SdkSuppress(minSdkVersion = 33)
class TabRowBlurTest {
    private val compose = createComposeRule()

    @get:Rule val rule: RuleChain = RuleChain.outerRule(object : ExternalResource() {
        override fun before() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.uiAutomation.executeShellCommand(
                "am start -W -n ${instrumentation.targetContext.packageName}/androidx.activity.ComponentActivity",
            ).use { descriptor ->
                ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
            }
        }
    }).around(compose)

    @Test fun standardRowExposesBackdropButKeepsSelectedTabSolid() = verifyBackdrop(false)
    @Test fun contourRowExposesBackdropButKeepsSelectedTabSolid() = verifyBackdrop(true)

    private fun verifyBackdrop(contour: Boolean) {
        val blur = mutableStateOf(false)
        val dark = mutableStateOf(false)
        val background = mutableStateOf(Color.Blue)
        val selected = mutableStateOf(0)
        lateinit var view: View
        compose.setContent {
            val localView = LocalView.current
            SideEffect { view = localView }
            C001apkComposeTheme(materialYou = false, darkTheme = dark.value) {
                MiuixScaffold(
                    blurBars = blur.value,
                    topBar = {
                        Column {
                            MiuixTopBar(title = "标签背景")
                            MiuixTabRow(
                                tabs = listOf("One", "Two"),
                                selectedTabIndex = selected.value,
                                onTabSelected = { selected.value = it },
                                withContour = contour,
                            )
                        }
                    },
                ) {
                    // Simulate content that has scrolled underneath the header.
                    Box(Modifier.fillMaxSize().background(background.value))
                }
            }
        }

        fun sample(): Pair<Int, Int> {
            compose.waitForIdle()
            SystemClock.sleep(400)
            val offset = IntArray(2)
            compose.runOnIdle { view.getLocationOnScreen(offset) }
            val bitmap = checkNotNull(
                InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot(),
            )
            fun Bitmap.tabPixel(label: String): Int {
                val bounds = compose.onNodeWithText(label).fetchSemanticsNode().boundsInRoot
                // Sample below the label, away from glyphs and rounded edges.
                return getPixel(
                    offset[0] + bounds.center.x.toInt(),
                    offset[1] + (bounds.bottom - 8).toInt(),
                )
            }
            return try { bitmap.tabPixel("One") to bitmap.tabPixel("Two") }
                finally { bitmap.recycle() }
        }

        for (night in listOf(false, true)) {
            compose.runOnIdle {
                dark.value = night
                blur.value = false
                background.value = Color.Blue
            }
            val opaque = sample()
            compose.runOnIdle { blur.value = true }
            val blue = sample()
            compose.runOnIdle { background.value = Color.Red }
            val red = sample()
            assertEquals("Selected tab must keep its solid default color", opaque.first, blue.first)
            assertEquals("Background changes must not tint the selected tab", blue.first, red.first)
            assertTrue("Blue content must reach the row backdrop", AndroidColor.blue(blue.second) > AndroidColor.red(blue.second) + 15)
            assertTrue("Backdrop must update with red content", AndroidColor.red(red.second) > AndroidColor.blue(red.second) + 15)
            compose.runOnIdle { blur.value = false }
            assertEquals("Disabling blur must restore the default surface", opaque, sample())
        }
        compose.onNodeWithText("Two").performClick()
        compose.runOnIdle { assertEquals(1, selected.value) }
    }
}
