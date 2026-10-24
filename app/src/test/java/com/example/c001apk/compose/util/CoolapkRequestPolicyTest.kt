package com.example.c001apk.compose.util

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Test

class CoolapkRequestPolicyTest {
    @Test
    fun apiCredentialsRequireExactHttpsOrigin() {
        listOf("https://api.coolapk.com/v6/main/indexV8", "https://api2.coolapk.com/v6/feed/detail")
            .forEach {
                assertTrue(CoolapkRequestPolicy.isApi(it.toHttpUrl()))
            }
        listOf(
                "http://api.coolapk.com/v6/",
                "https://api.coolapk.com:8443/v6/",
                "https://api.coolapk.com.attacker.test/v6/",
                "https://image.coolapk.com/",
                "https://account.coolapk.com/",
                "https://raw.githubusercontent.com/",
            )
            .forEach {
                assertFalse(it, CoolapkRequestPolicy.isApi(it.toHttpUrl()))
            }
    }

    @Test
    fun publicSignatureIsLimitedToNumericFeedGet() {
        fun request(path: String) = Request.Builder().url("https://www.coolapk.com$path").build()
        assertTrue(CoolapkRequestPolicy.isPublicFeed(request("/feed/123")))
        listOf("/feed/abc", "/feed/123/", "/feed/123/../login", "/feed/%31%32%33", "/u/123")
            .forEach {
                assertFalse(it, CoolapkRequestPolicy.isPublicFeed(request(it)))
            }
        assertFalse(
            CoolapkRequestPolicy.isPublicFeed(request("/feed/123").newBuilder().head().build()),
        )
        assertFalse(
            CoolapkRequestPolicy.isPublicFeed(
                Request.Builder().url("http://www.coolapk.com/feed/123").build(),
            ),
        )
    }

    @Test
    fun redirectSanitizationStripsCredentialsAndDeviceIdentity() {
        val headers =
            listOf(
                "Cookie",
                "Authorization",
                "User-Agent",
                "X-App-Device",
                "x-app-token",
                "X-Requested-With",
                "X-Sdk-Int",
                "X-Api-Version",
                "X-Dark-Mode",
            )
        val builder =
            Request.Builder().url("https://cdn.example.test/image.jpg").header("Accept", "image/*")
        headers.forEach { builder.header(it, "private-test-value") }
        val clean = CoolapkRequestPolicy.withoutPrivateHeaders(builder.build()).build()
        headers.forEach { assertNull(it, clean.header(it)) }
        assertEquals("image/*", clean.header("Accept"))
    }

    @Test
    fun accountCookiesCannotBeSentToApiCdnOrInsecureHosts() {
        assertTrue(
            CoolapkRequestPolicy.isAccount("https://account.coolapk.com/auth/login".toHttpUrl()),
        )
        listOf(
                "https://api.coolapk.com/",
                "http://account.coolapk.com/",
                "https://account.coolapk.com:444/",
                "https://account.coolapk.com.attacker.test/",
            )
            .forEach {
                assertFalse(CoolapkRequestPolicy.isAccount(it.toHttpUrl()))
            }
    }
}
