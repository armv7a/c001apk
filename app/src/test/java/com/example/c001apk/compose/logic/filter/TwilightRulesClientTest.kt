package com.example.c001apk.compose.logic.filter

import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

class TwilightRulesClientTest {
    private val json = """{"schemaVersion":1,"revision":1,"publishedAt":"2026-10-26T00:00:00Z","rules":[]}"""
    private fun response(body: ResponseBody = json.toResponseBody("application/json".toMediaType()), code: Int = 200): Response =
        Response.Builder().request(Request.Builder().url(TwilightRulesClient.URL).build())
            .protocol(Protocol.HTTP_1_1).code(code).message("test").body(body).build()

    @Test fun independentDownloadUsesOnlyPublicRequestHeaders() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            assertEquals(TwilightRulesClient.URL, chain.request().url.toString())
            assertEquals(setOf("Accept", "User-Agent"), chain.request().headers.names())
            response().newBuilder().request(chain.request()).build()
        }.build()
        assertEquals(1, TwilightRulesClient(client).fetch().revision)
    }

    @Test fun rejectsHttpFailuresHtmlMalformedUtf8AndOversizedStreams() {
        val oversized = object : ResponseBody() {
            override fun contentType() = "application/json".toMediaType()
            override fun contentLength() = -1L
            override fun source() = Buffer().write(ByteArray(TwilightRuleSet.MAX_BYTES + 1) { 32 })
        }
        val invalid = listOf(response(code = 302), response(code = 500),
            response(json.toResponseBody("text/html".toMediaType())),
            response(byteArrayOf(0xc3.toByte(), 0x28).toResponseBody("application/json".toMediaType())), response(oversized))
        invalid.forEach { assertTrue(runCatching { it.use(TwilightRulesClient::readRules) }.isFailure) }
        assertEquals(1, response(json.toResponseBody("text/plain".toMediaType())).use(TwilightRulesClient::readRules).revision)
    }
}
