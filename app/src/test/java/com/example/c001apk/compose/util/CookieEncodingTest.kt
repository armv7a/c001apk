package com.example.c001apk.compose.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CookieEncodingTest {
    @Test
    fun profileNamesRoundTripWithoutDoubleEscaping() {
        for (name in listOf("中文 用户", "C++", "100%", "literal%20+%2B", "user_tag:%s%d", "")) {
            assertEquals(name, decodeCookieValue(encodeCookieValue(name)))
        }
        assertEquals("C%2B%2B", encodeCookieValue("C++"))
        assertEquals("100%25", encodeCookieValue("100%"))
        assertEquals("", encodeCookieValue(null))
    }

    @Test
    fun decodesValidCookieUsernamesExactlyOnce() {
        assertEquals("测试 用户+100%", decodeCookieValue("%E6%B5%8B%E8%AF%95+%E7%94%A8%E6%88%B7%2B100%25"))
        assertEquals("%20 +", decodeCookieValue("%2520+%2B"))
        assertEquals("", decodeCookieValue(""))
    }

    @Test
    fun malformedCookieKeepsItsOriginalValueWithoutCrashing() {
        for (value in listOf("user_tag:%s%d", "%", "%2", "%GG", "%E6%B5%8B+100%")) {
            assertEquals(value, decodeCookieValue(value))
        }
    }
}
