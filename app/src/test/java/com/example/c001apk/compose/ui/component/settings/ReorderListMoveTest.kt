package com.example.c001apk.compose.ui.component.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ReorderListMoveTest {
    @Test
    fun exactMidpointDoesNotOscillateBetweenTwoSlots() {
        assertEquals(0 to 50f, dragReorderTarget(0, listOf(100, 100, 100), 0f, 50f))
        assertEquals(1 to -49f, dragReorderTarget(0, listOf(100, 100, 100), 0f, 51f))
    }

    @Test
    fun unequalHeightsCannotSwapBackInTheSamePointerEvent() {
        assertEquals(1 to -39f, dragReorderTarget(0, listOf(20, 80, 100), 0f, 41f))
        assertEquals(0 to 39f, dragReorderTarget(1, listOf(80, 20, 100), 0f, -41f))
    }

    @Test
    fun fastDragsMoveMultipleRowsAndStopAtTheCardBoundary() {
        assertEquals(2 to 0f, dragReorderTarget(0, listOf(100, 100, 100), 0f, 500f))
        assertEquals(0 to 0f, dragReorderTarget(2, listOf(100, 100, 100), 0f, -500f))
    }

    @Test
    fun continuedMotionDoesNotUndoThePreviousSwapUntilFingerReverses() {
        assertEquals(1 to -48f, dragReorderTarget(1, listOf(100, 100, 100), -49f, 1f))
        assertEquals(0 to 49f, dragReorderTarget(1, listOf(100, 100, 100), -49f, -2f))
    }


    @Test
    fun movingDownKeepsOtherEntriesInOrder() {
        assertEquals(listOf("b", "c", "a", "d"), listOf("a", "b", "c", "d").moveItem(0, 2))
    }

    @Test
    fun movingUpKeepsOtherEntriesInOrder() {
        assertEquals(listOf("a", "d", "b", "c"), listOf("a", "b", "c", "d").moveItem(3, 1))
    }

    @Test
    fun movingByOneSwapsNeighbours() {
        assertEquals(listOf("b", "a", "c"), listOf("a", "b", "c").moveItem(0, 1))
        assertEquals(listOf("a", "c", "b"), listOf("a", "b", "c").moveItem(2, 1))
    }

    @Test
    fun invalidOrNoOpMovesKeepTheSameList() {
        val list = listOf("a", "b", "c")
        assertSame(list, list.moveItem(1, 1))
        assertSame(list, list.moveItem(-1, 0))
        assertSame(list, list.moveItem(0, 3))
        assertSame(list, list.moveItem(3, 0))
    }

    @Test
    fun oneElementListIsUnchanged() {
        val list = listOf("only")
        assertSame(list, list.moveItem(0, 0))
    }
}
