package com.example.c001apk.compose.logic.smid

import cn.shuzilm.core.DigitalUnionEndpoint.toNativeAuthority
import org.junit.Assert.*
import org.junit.Test

class DigitalUnionEndpointTest {
    @Test
    fun httpsFallbackUsesAnExplicitTlsPort() {
        assertEquals("global-auni.checktrustworthiness.com:443", toNativeAuthority(DIGITAL_UNION_FALLBACK_URL))
        assertEquals("global-auni.checktrustworthiness.com:443", DIGITAL_UNION_FALLBACK_ENDPOINT)
    }

    @Test
    fun normalizesSupportedEndpointFormsWithoutChangingTheHost() {
        mapOf(
            " HTTPS://Example.COM/ " to "example.com:443",
            "https://example.com:8443" to "example.com:8443",
            "http://example.com" to "example.com:80",
            "example.com" to "example.com",
            "Example.COM:00443" to "example.com:443",
            "127.0.0.1:65535" to "127.0.0.1:65535",
        ).forEach { (input, expected) ->
            assertEquals(input, expected, toNativeAuthority(input))
            assertEquals(expected, toNativeAuthority(expected))
        }
        assertNull(toNativeAuthority(null))
    }

    @Test
    fun rejectsValuesTheNativeHostPortParserCannotRepresent() {
        listOf(
            "", " ", "https://", "https://example.com/api", "https://example.com?x=1",
            "https://example.com/#fragment", "https://user:password@example.com",
            "ftp://example.com", "example.com:0", "example.com:65536", "example.com:-1",
            "example.com:999999999999999999999999", "example.com:", "example.com:abc",
            "https://[::1]:443", "2001:db8::1", "https://evil.com%2f.example.com",
            "bad\nhost:443", "a..example.com", "-bad.example.com", "a".repeat(64) + ".com",
        ).forEach { input ->
            assertThrows(input, IllegalArgumentException::class.java) { toNativeAuthority(input) }
        }
    }
}
