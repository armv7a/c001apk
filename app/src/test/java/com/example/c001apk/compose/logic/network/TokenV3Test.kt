package com.example.c001apk.compose.logic.network

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class TokenV3Test {
    @Test
    fun matchesIndependentFixedTimeVectors() {
        val vectors =
            JsonParser.parseReader(javaClass.getResourceAsStream("/token_v3.json")!!.reader())
                .asJsonArray
        for (vector in vectors) {
            val item = vector.asJsonObject
            assertEquals(
                item["token"].asString,
                TokenV3.create(item["device"].asString, timestamp = item["timestamp"].asLong),
            )
        }
    }

    @Test
    fun timestampAndAppCodeChangeSignature() {
        val first = TokenV3.create("synthetic-test-device", timestamp = 1770000000)
        assertNotEquals(first, TokenV3.create("synthetic-test-device", timestamp = 1770000001))
        assertNotEquals(
            first,
            TokenV3.create("synthetic-test-device", appCode = 2604202, timestamp = 1770000000),
        )
    }
}
