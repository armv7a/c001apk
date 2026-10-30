package com.example.c001apk.compose.logic.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MainNavigationOrderTest {
    @Test
    fun emptyAndUnknownOrdersFallBackToAllDefaultPages() {
        assertEquals(listOf(0, 1, 2), MainNavigationOrder.normalize(emptyList()))
        assertEquals(listOf(0, 1, 2), MainNavigationOrder.normalize(listOf(-1, 3, 99)))
    }

    @Test
    fun partialOrdersKeepFirstOccurrenceAndAppendMissingPages() {
        assertEquals(listOf(2, 0, 1), MainNavigationOrder.normalize(listOf(2, 2, -1, 0)))
        assertEquals(listOf(1, 0, 2), MainNavigationOrder.normalize(listOf(1)))
    }

    @Test
    fun allCompleteOrdersAreStableAndNormalizationIsIdempotent() {
        val orders = listOf(
            listOf(0, 1, 2), listOf(0, 2, 1), listOf(1, 0, 2),
            listOf(1, 2, 0), listOf(2, 0, 1), listOf(2, 1, 0),
        )
        for (order in orders) {
            val normalized = MainNavigationOrder.normalize(order)
            assertEquals(order, normalized)
            assertEquals(normalized, MainNavigationOrder.normalize(normalized))
            for (page in order) assertEquals(page, order[order.indexOf(page)])
        }
    }
}
