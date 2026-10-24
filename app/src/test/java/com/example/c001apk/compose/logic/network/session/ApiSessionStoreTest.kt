package com.example.c001apk.compose.logic.network.session

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class ApiSessionStoreTest {
    private val url = "https://api.coolapk.com/v6/feed/detail".toHttpUrl()
    private var identity = ApiSessionStore.Identity(1, "synthetic-account-and-device")
    private var time = System.currentTimeMillis()
    private var saved: String? = null
    private fun store() = ApiSessionStore({ saved }, { saved = it }, { identity }, { time })
    @Test fun sessionSurvivesRestartButRespectsOriginPathAndExpiry() {
        var session = store()
        session.save(url, listOf("SESSID=synthetic; Domain=.coolapk.com; Path=/v6; Max-Age=999999"), session.capture())
        session = store()
        assertEquals("SESSID=synthetic", session.cookieFor("https://api2.coolapk.com/v6/feed/detail".toHttpUrl()))
        listOf("http://api.coolapk.com/v6/feed", "https://cdn.coolapk.com/v6/image", "https://api.coolapk.com/v66/feed").forEach {
            assertEquals("", session.cookieFor(it.toHttpUrl()))
        }
        time += 7 * 86400000L + 1
        assertEquals("", session.cookieFor(url))
    }
    @Test fun staleResponsesAndWrongIdentityCannotOverwriteSession() {
        val session = store()
        val old = session.capture()
        session.save(url, listOf("SESSID=newer; Path=/"), old)
        assertNull(session.save(url, listOf("SESSID=stale; Path=/"), old))
        assertEquals("SESSID=newer", session.cookieFor(url))
        identity = ApiSessionStore.Identity(2, "another-device")
        assertEquals("", session.cookieFor(url))
        assertNull(session.save(url, listOf("SESSID=stale; Path=/"), old))
    }
    @Test fun validationRequiresAcceptedCurrentSessionAndExpiresAfterOneDay() {
        var session = store()
        session.save(url, listOf("SESSID=synthetic; Path=/"), session.capture())
        val context = session.capture()
        session.acceptValidation("synthetic-proof", context, "wrong-session")
        assertNull(session.validation())
        session.acceptValidation("synthetic-proof", context, session.hash())
        session = store()
        assertEquals("synthetic-proof", session.validation())
        time += 86400000L + 1
        assertNull(session.validation())
    }
    @Test fun sessionReplacementAndLogoutClearValidation() {
        val session = store()
        session.acceptValidation("synthetic-proof", session.capture(), session.hash())
        session.save(url, listOf("SESSID=replaced; Path=/"), session.capture())
        assertNull(session.validation())
        session.acceptValidation("synthetic-proof", session.capture(), session.hash())
        session.clear()
        assertNull(session.validation())
        assertEquals("", session.cookieFor(url))
    }
}
