package com.example.c001apk.compose.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.example.c001apk.compose.ui.component.currentWindowSize
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ContentScaleTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun changingContentScaleKeepsWindowWidthAndRightAlignedContentFlush() {
        val scale = mutableFloatStateOf(1f)
        var rootWidth = 0
        var contentWidth = 0
        var contentLeft = 0f
        compose.setContent {
            C001apkComposeTheme(materialYou = false, contentScale = scale.floatValue) {
                val windowSize = currentWindowSize()
                Box(Modifier.fillMaxSize().onGloballyPositioned { rootWidth = it.size.width }) {
                    Box(
                        Modifier.width(windowSize.width)
                            .align(Alignment.CenterEnd)
                            .onGloballyPositioned {
                                contentWidth = it.size.width
                                contentLeft = it.positionInRoot().x
                            },
                    )
                }
            }
        }
        for (value in listOf(1f, 0.8f, 0.95f, 1.3f, 1f)) {
            compose.runOnIdle { scale.floatValue = value }
            compose.runOnIdle {
                assertTrue("The root must be measured", rootWidth > 0)
                assertEquals("Window width at scale $value", rootWidth, contentWidth)
                assertEquals("Left edge at scale $value", 0f, contentLeft, 1f)
            }
        }
    }
}
