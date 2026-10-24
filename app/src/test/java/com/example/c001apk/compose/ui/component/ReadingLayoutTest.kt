package com.example.c001apk.compose.ui.component

import org.junit.Assert.*
import org.junit.Test

class ReadingLayoutTest {
    @Test
    fun columnsRespectWidthTextScaleAndUpperBound() {
        assertEquals(1, ReadingLayout.columns(400f, 1f))
        assertEquals(2, ReadingLayout.columns(800f, 1f))
        assertEquals(1, ReadingLayout.columns(800f, 2f))
        assertEquals(4, ReadingLayout.columns(4000f, 1f))
    }

    @Test
    fun splitRespectsExplicitModeAndPhoneOrientation() {
        assertFalse(ReadingLayout.split(1000f, 400f, 1f, 0))
        assertTrue(ReadingLayout.split(1000f, 400f, 1f, 1))
        assertFalse(ReadingLayout.split(400f, 400f, 1f, 1))
        assertTrue(ReadingLayout.split(1000f, 700f, 1f, 0))
        assertFalse(ReadingLayout.split(1000f, 700f, 1f, 2))
        assertFalse(ReadingLayout.split(1000f, 700f, 1.5f, 0))
    }
}
