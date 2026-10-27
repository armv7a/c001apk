package com.example.c001apk.compose.ui.main

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.*
import org.junit.Test

class MainStartupStateTest {
    @Test
    fun localAccountIsReadyBeforeNavigationOrIdentityLookup() = runBlocking {
        val state = MainStartupState()
        val accountReady = CompletableDeferred<Unit>()
        var lookupStarted = false
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            state.prepare(
                initializeAccount = { accountReady.await() },
                ensureId = {
                    assertFalse(state.preparingAccount)
                    lookupStarted = true
                    Result.success("cached-id")
                },
            )
        }
        assertTrue(state.preparingAccount)
        assertFalse(lookupStarted)
        accountReady.complete(Unit)
        job.join()
        assertFalse(state.preparingAccount)
        assertTrue(lookupStarted)
    }

    @Test
    fun pendingNetworkRequestDoesNotHoldTheLaunchScreen() = runBlocking {
        val state = MainStartupState()
        val id = CompletableDeferred<Result<String>>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            state.prepare(initializeAccount = {}, ensureId = { id.await() })
        }
        assertFalse(state.preparingAccount)
        assertTrue(job.isActive)
        id.complete(Result.success("new-id"))
        job.join()
        assertNull(withTimeoutOrNull(50) { state.identityFailures.first() })
    }

    @Test
    fun failureKeepsHomeAccessibleAndWaitsForOneForegroundCollector() = runBlocking {
        val state = MainStartupState()
        state.prepare({}, { Result.failure(IOException("both endpoints failed")) })
        assertFalse(state.preparingAccount)
        // The UI may be stopped when the request fails, or recreated after consuming the toast.
        assertEquals(Unit, withTimeout(1_000) { state.identityFailures.first() })
        assertNull(withTimeoutOrNull(50) { state.identityFailures.first() })
    }

    @Test
    fun cachedIdentityDoesNotProduceAFailureNotification() = runBlocking {
        val state = MainStartupState()
        state.prepare({}, { Result.success("cached-id") })
        assertFalse(state.preparingAccount)
        assertNull(withTimeoutOrNull(50) { state.identityFailures.first() })
    }

    @Test
    fun leavingDuringRegistrationDoesNotProduceAFailureNotification() = runBlocking {
        val state = MainStartupState()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            state.prepare({}, { awaitCancellation() })
        }
        assertFalse(state.preparingAccount)
        job.cancelAndJoin()
        assertNull(withTimeoutOrNull(50) { state.identityFailures.first() })
    }
}
