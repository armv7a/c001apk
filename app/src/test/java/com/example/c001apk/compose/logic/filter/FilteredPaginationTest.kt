package com.example.c001apk.compose.logic.filter

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FilteredPaginationTest {
    @Test fun hiddenBatchPausesAfterThreePagesAndManualRetryResumesAtNextPage() = runBlocking {
        val cached = mutableListOf<FilterInput>()
        val engine = ContentFilterEngine(ContentFilterPreferences(enabled = true, rules = listOf(ContentRule("r", "规则", terms = listOf("广告")))))
        var page = 0
        suspend fun request() = loadFilteredPages(fetch = {
            page++
            FilterInput(FilterScope.FEED, "FEED:$page", "1", if (page <= 3) "广告" else "内容")
        }, accept = { row, _ -> cached += row; engine.match(row) == null })
        assertTrue(request())
        assertEquals(3, page)
        assertEquals(3, cached.size)
        assertFalse(request())
        assertEquals(4, page)
        // No refetch is needed to restore the hidden content after disabling filtering.
        assertEquals(4, cached.count { ContentFilterEngine(ContentFilterPreferences()).match(it) == null })
    }

    @Test fun terminalOrErrorResponseStopsBatchAndCancellationPropagates() = runBlocking {
        var requests = 0
        assertFalse(loadFilteredPages(fetch = { requests++; "error" }, accept = { _, _ -> true }))
        assertEquals(1, requests)
        assertTrue(runCatching {
            loadFilteredPages<Unit>(fetch = { throw CancellationException("cancel") }, accept = { _, _ -> false })
        }.exceptionOrNull() is CancellationException)
    }
}
