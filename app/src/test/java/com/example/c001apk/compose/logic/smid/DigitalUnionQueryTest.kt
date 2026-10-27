package com.example.c001apk.compose.logic.smid

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.*
import org.junit.Test

class DigitalUnionQueryTest {
    private val validId = "synthetic-device-id"

    @Test
    fun primarySuccessDoesNotContactFallback() = runBlocking {
        val urls = mutableListOf<String?>()
        val id = queryDigitalUnionId {
            urls += it
            "  $validId  "
        }
        assertEquals(validId, id)
        assertEquals(listOf<String?>(null), urls)
    }

    @Test
    fun nativeRegistrationCanCompleteOnALaterPrimaryPoll() = runBlocking {
        var calls = 0
        val id = pollDigitalUnionId(retryDelayMillis = 0) {
            if (++calls == 3) validId else null
        }
        assertEquals(validId, id)
        assertEquals(3, calls)
    }

    @Test
    fun invalidPrimaryResultsUseTheFallbackUrl() = runBlocking {
        val urls = mutableListOf<String?>()
        val id = queryDigitalUnionId { url ->
            urls += url
            if (url == null) pollDigitalUnionId(retryDelayMillis = 0) { "too-short" } else validId
        }
        assertEquals(validId, id)
        assertEquals(listOf(null, DIGITAL_UNION_FALLBACK_ENDPOINT), urls)
    }

    @Test
    fun primaryTimeoutStillLeavesTimeForFallback() = runBlocking {
        val urls = mutableListOf<String?>()
        val id = queryDigitalUnionId { url ->
            urls += url
            if (url == null) withTimeoutOrNull(100) { awaitCancellation() } else validId
        }
        assertEquals(validId, id)
        assertEquals(listOf(null, DIGITAL_UNION_FALLBACK_ENDPOINT), urls)
    }

    @Test
    fun primaryExceptionsCanRecoverViaFallback() = runBlocking {
        val id = queryDigitalUnionId { url ->
            if (url == null) throw IOException("synthetic failure") else validId
        }
        assertEquals(validId, id)
    }

    @Test
    fun bothEndpointsFailWithoutReturningAnInvalidId() = runBlocking {
        val urls = mutableListOf<String?>()
        val id = queryDigitalUnionId {
            urls += it
            "null"
        }
        assertNull(id)
        assertEquals(listOf(null, DIGITAL_UNION_FALLBACK_ENDPOINT), urls)
    }

    @Test
    fun cancellationDoesNotStartFallback() = runBlocking {
        var calls = 0
        val cancelled = CancellationException("caller left")
        try {
            queryDigitalUnionId {
                calls++
                throw cancelled
            }
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            // Coroutine debug stack recovery may copy exceptions across suspension points.
            assertEquals(cancelled.message, actual.message)
        }
        assertEquals(1, calls)
    }

    @Test
    fun invalidPollsAreBoundedAndNeverReturnAnId() = runBlocking {
        val values = listOf(null, " ", "null", "too-short")
        var calls = 0
        assertNull(pollDigitalUnionId(retryDelayMillis = 0) { values[calls++] })
        assertEquals(4, calls)
    }

    @Test
    fun pollExceptionsCanRecoverButCancellationStopsImmediately() = runBlocking {
        var calls = 0
        assertEquals(validId, pollDigitalUnionId(retryDelayMillis = 0) {
            if (++calls < 3) throw IOException("synthetic") else validId
        })
        calls = 0
        try {
            pollDigitalUnionId(retryDelayMillis = 0) {
                calls++
                throw CancellationException("caller left")
            }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            assertEquals(1, calls)
        }
    }

    @Test
    fun nextQueryStartsWithPrimaryAgain() = runBlocking {
        val endpoints = mutableListOf<String?>()
        repeat(2) {
            assertEquals(validId, queryDigitalUnionId { endpoint ->
                endpoints += endpoint
                if (endpoint == null) null else validId
            })
        }
        assertEquals(listOf(null, DIGITAL_UNION_FALLBACK_ENDPOINT, null, DIGITAL_UNION_FALLBACK_ENDPOINT), endpoints)
    }
}
