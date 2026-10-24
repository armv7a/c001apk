package com.example.c001apk.compose.logic.follow

import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.state.LoadingState
import com.google.gson.Gson
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class LocalFollowPagerTest {
    private fun feed(id: String, uid: String, time: Long) =
        Gson()
            .fromJson(
                """{"id":"$id","uid":"$uid","dateline":$time,"entityType":"feed"}""",
                HomeFeedResponse.Data::class.java,
            )

    @Test
    fun mergesByTimeAndDeduplicatesAcrossPages() = runBlocking {
        val pages =
            mapOf(
                "a" to
                    listOf(
                        listOf(feed("3", "a", 30), feed("1", "a", 10)),
                        listOf(feed("1", "a", 10), feed("0", "a", 0)),
                    ),
                "b" to listOf(listOf(feed("2", "b", 20))),
            )
        val pager =
            LocalFollowPager(listOf("a", "b", "a")) { uid, page, _ ->
                pages[uid]?.getOrNull(page - 1)?.let { LoadingState.Success(it) }
                    ?: LoadingState.Empty
            }
        assertEquals(listOf("3", "2"), pager.next(2).map { it.id })
        assertEquals(listOf("1", "0"), pager.next(20).map { it.id })
        assertTrue(pager.isEnd)
    }

    @Test
    fun failedUserCanRetryWithoutSkippingItsPage() = runBlocking {
        var failed = true
        val requests = mutableListOf<Pair<String, Int>>()
        val pager =
            LocalFollowPager(listOf("a", "b")) { uid, page, _ ->
                requests += uid to page
                when {
                    uid == "a" && failed -> LoadingState.Error("retry")
                    page == 1 -> LoadingState.Success(listOf(feed(uid, uid, 1)))
                    else -> LoadingState.Empty
                }
            }
        assertEquals(listOf("b"), pager.next().map { it.id })
        assertNotNull(pager.warning)
        assertFalse(pager.isEnd)
        failed = false
        assertEquals(listOf("a"), pager.next().map { it.id })
        assertEquals(2, requests.count { it == "a" to 1 })
        assertTrue(pager.isEnd)
    }

    @Test
    fun filteredPagesStopAtBoundAndResumeOnNextRequest() = runBlocking {
        var calls = 0
        val pager =
            LocalFollowPager(listOf("a")) { _, page, _ ->
                calls++
                when {
                    page < 4 -> LoadingState.Success(emptyList())
                    page == 4 -> LoadingState.Success(listOf(feed("x", "a", 1)))
                    else -> LoadingState.Empty
                }
            }
        assertTrue(pager.next().isEmpty())
        assertEquals(3, calls)
        assertFalse(pager.isEnd)
        assertNotNull(pager.warning)
        assertEquals(listOf("x"), pager.next().map { it.id })
    }

    @Test
    fun boundsConcurrencyAndPropagatesCancellation() = runBlocking {
        val active = AtomicInteger()
        val maximum = AtomicInteger()
        val pager =
            LocalFollowPager((1..12).map(Int::toString)) { _, _, _ ->
                val count = active.incrementAndGet()
                maximum.updateAndGet { maxOf(it, count) }
                try {
                    delay(10)
                    LoadingState.Empty
                } finally {
                    active.decrementAndGet()
                }
            }
        pager.next()
        assertTrue(maximum.get() in 1..4)
        val cancelling =
            LocalFollowPager(listOf("a")) { _, _, _ -> throw CancellationException("cancel") }
        assertTrue(runCatching { cancelling.next() }.exceptionOrNull() is CancellationException)
    }
}
