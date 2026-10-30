package com.example.c001apk.compose.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTouchInput
import com.example.c001apk.compose.ui.component.settings.ReorderablePreferenceGroup
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Text

class ReorderablePreferenceGroupInteractionTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setRows(
        initial: List<String>,
        enabled: Boolean = true,
    ): MutableState<List<String>> {
        val order = mutableStateOf(initial)
        compose.setContent {
            C001apkComposeTheme(materialYou = false) {
                Column(Modifier.testTag("reorder-root")) {
                    ReorderablePreferenceGroup(
                        items = order.value,
                        key = { it },
                        onReorder = { order.value = it },
                        title = "顺序",
                        enabled = enabled,
                    ) { item, _, dragHandle ->
                        BasicComponent(
                            title = item,
                            modifier = Modifier.testTag("row_$item"),
                            endActions = { dragHandle() },
                        )
                    }
                    Text(order.value.joinToString(","), Modifier.testTag("order"))
                }
            }
        }
        return order
    }

    private fun dragFirstHandleOneRowDown() {
        val rowHeight =
            compose
                .onNodeWithTag("row_A", useUnmergedTree = true)
                .fetchSemanticsNode()
                .size.height
                .toFloat()
        compose
            .onAllNodesWithContentDescription("拖动排序", useUnmergedTree = true)[0]
            .performTouchInput {
                down(center)
                repeat(10) { moveBy(Offset(0f, rowHeight / 10f)) }
                up()
            }
        compose.waitForIdle()
    }

    @Test
    fun draggingTheHandleMovesOneRowDown() {
        val order = setRows(listOf("A", "B", "C"))
        compose.onNodeWithTag("order", useUnmergedTree = true).assertTextEquals("A,B,C")

        dragFirstHandleOneRowDown()

        compose.onNodeWithTag("order", useUnmergedTree = true).assertTextEquals("B,A,C")
        assertEquals(listOf("B", "A", "C"), order.value)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun accessibilityActionsMoveRowsWithoutDragging() {
        val order = setRows(listOf("A", "B", "C"))

        compose
            .onAllNodesWithContentDescription("拖动排序", useUnmergedTree = true)[0]
            .performCustomAccessibilityActionWithLabel("下移")
        compose.waitForIdle()

        compose.onNodeWithTag("order", useUnmergedTree = true).assertTextEquals("B,A,C")
        assertEquals(listOf("B", "A", "C"), order.value)

        compose
            .onAllNodesWithContentDescription("拖动排序", useUnmergedTree = true)[1]
            .performCustomAccessibilityActionWithLabel("上移")
        compose.waitForIdle()

        compose.onNodeWithTag("order", useUnmergedTree = true).assertTextEquals("A,B,C")
        assertEquals(listOf("A", "B", "C"), order.value)
    }

    @Test
    fun aSecondHandleCannotTakeOverAnActiveDrag() {
        val order = setRows(listOf("A", "B", "C"))
        val handles = compose.onAllNodesWithContentDescription("拖动排序", useUnmergedTree = true)
        val first = handles[0].fetchSemanticsNode().boundsInRoot.center
        val second = handles[1].fetchSemanticsNode().boundsInRoot.center
        val third = handles[2].fetchSemanticsNode().boundsInRoot.center
        val root = compose.onNodeWithTag("reorder-root", useUnmergedTree = true)
        root.performTouchInput {
            down(pointerId = 0, position = first)
            repeat(10) { moveBy(pointerId = 0, delta = Offset(0f, (second.y - first.y) / 10f)) }
            down(pointerId = 1, position = third)
            repeat(10) { moveBy(pointerId = 1, delta = Offset(0f, (first.y - third.y) / 10f)) }
            up(pointerId = 1)
            up(pointerId = 0)
        }
        compose.waitForIdle()
        assertEquals(listOf("B", "A", "C"), order.value)
    }

    @Test
    fun externalUpdateDuringDragIsNotOverwrittenByTheGestureSnapshot() {
        val order = setRows(listOf("A", "B", "C"))
        val handles = compose.onAllNodesWithContentDescription("拖动排序", useUnmergedTree = true)
        val first = handles[0].fetchSemanticsNode().boundsInRoot.center
        val second = handles[1].fetchSemanticsNode().boundsInRoot.center
        val root = compose.onNodeWithTag("reorder-root", useUnmergedTree = true)
        root.performTouchInput {
            down(first)
            repeat(10) { moveBy(Offset(0f, (second.y - first.y) / 10f)) }
        }
        compose.runOnIdle { order.value = listOf("C", "B", "A") }
        root.performTouchInput { moveBy(Offset(0f, 50f)); up() }
        compose.waitForIdle()
        assertEquals(listOf("C", "B", "A"), order.value)
    }

    @Test
    fun cancelRestoresOrderBeforeDragging() {
        val order = setRows(listOf("A", "B", "C"))
        val height = compose.onNodeWithTag("row_A", useUnmergedTree = true).fetchSemanticsNode().size.height.toFloat()
        compose.onAllNodesWithContentDescription("拖动排序", useUnmergedTree = true)[0].performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(0f, height / 10f)) }
            cancel()
        }
        compose.waitForIdle()
        assertEquals(listOf("A", "B", "C"), order.value)
    }

    @Test
    fun disabledGroupIgnoresTheHandle() {
        val order = setRows(listOf("A", "B", "C"), enabled = false)

        dragFirstHandleOneRowDown()

        compose.onNodeWithTag("order", useUnmergedTree = true).assertTextEquals("A,B,C")
        assertEquals(listOf("A", "B", "C"), order.value)
    }
}
