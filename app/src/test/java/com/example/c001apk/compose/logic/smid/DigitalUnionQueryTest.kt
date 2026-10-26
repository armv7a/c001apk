package com.example.c001apk.compose.logic.smid

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DigitalUnionQueryTest {
    private val validId = "synthetic-device-id"

    @Test
    fun primarySuccessDoesNotContactFallback() = runBlocking {
        val urls = mutableListOf<String?>()
        val id = queryDigitalUnionId(retryDelayMillis = 0) {
            urls += it
            "  $validId  "
        }
        assertEquals(validId, id)
        assertEquals(listOf<String?>(null), urls)
    }

    @Test
    fun nativeRegistrationCanCompleteOnALaterPrimaryPoll() = runBlocking {
        var calls = 0
        val id = queryDigitalUnionId(retryDelayMillis = 0) { url ->
            assertNull(url)
            if (++calls == 3) validId else null
        }
        assertEquals(validId, id)
        assertEquals(3, calls)
    }

    @Test
    fun invalidPrimaryResultsUseTheFallbackUrl() = runBlocking {
        val results = listOf(null, "", "null", "too-short")
        val urls = mutableListOf<String?>()
        val id = queryDigitalUnionId(retryDelayMillis = 0) { url ->
            urls += url
            if (url == null) results[urls.lastIndex] else validId
        }
        assertEquals(validId, id)
        assertEquals(List<String?>(4) { null } + DIGITAL_UNION_FALLBACK_URL, urls)
    }

    @Test
    fun primaryTimeoutStillLeavesTimeForFallback() = runBlocking {
        val urls = mutableListOf<String?>()
        val id = queryDigitalUnionId(endpointTimeoutMillis = 100, retryDelayMillis = 0) { url ->
            urls += url
            if (url == null) awaitCancellation() else validId
        }
        assertEquals(validId, id)
        assertEquals(listOf(null, DIGITAL_UNION_FALLBACK_URL), urls)
    }

    @Test
    fun primaryExceptionsCanRecoverViaFallback() = runBlocking {
        val id = queryDigitalUnionId(retryDelayMillis = 0) { url ->
            if (url == null) throw IOException("synthetic failure") else validId
        }
        assertEquals(validId, id)
    }

    @Test
    fun bothEndpointsFailWithoutReturningAnInvalidId() = runBlocking {
        val urls = mutableListOf<String?>()
        val id = queryDigitalUnionId(retryDelayMillis = 0) {
            urls += it
            "null"
        }
        assertNull(id)
        assertEquals(List<String?>(4) { null } + List(4) { DIGITAL_UNION_FALLBACK_URL }, urls)
    }

    @Test
    fun cancellationDoesNotStartFallback() = runBlocking {
        var calls = 0
        val cancelled = CancellationException("caller left")
        try {
            queryDigitalUnionId(retryDelayMillis = 0) {
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
}
