package com.example.c001apk.compose.logic.smid

import org.junit.Assert.*
import org.junit.Test

class OfficialDeviceIdentityTest {
    @Test
    fun extractsOnlyUniqueNamedField() {
        assertEquals(
            "synthetic-id-123",
            OfficialDeviceIdentity.parse(
                "OAID: other\nAndroidID: ignored\nShuzlmID： synthetic-id-123\nUUID: ignored",
            ),
        )
        assertEquals(
            "synthetic-id-123",
            OfficialDeviceIdentity.parse("\uFEFFshuzlmid: synthetic-id-123"),
        )
    }

    @Test
    fun rejectsAmbiguityOtherIdsAndInvalidValues() {
        listOf(
                "OAID: synthetic-id-123",
                "ShuzlmID: null",
                "ShuzlmID: undefined",
                "ShuzlmID: has spaces",
                "ShuzlmID: first-id-123\nShuzlmID: second-id-123",
                "x".repeat(32769),
            )
            .forEach {
                try {
                    OfficialDeviceIdentity.parse(it)
                    fail("invalid input was accepted")
                } catch (_: IllegalArgumentException) {}
            }
    }
}
