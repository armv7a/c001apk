package com.example.c001apk.compose.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ImagePreviewUrlTest {
    @Test
    fun reportedLongImageUsesCompleteSourceInEveryQualityMode() {
        val original = "https://image.coolapk.com/feed/sample@640x6857.jpg"
        // The issue's .s.jpg response contains only 640 x 1760 pixels.
        for (preview in listOf(original, "$original.s.jpg")) {
            assertEquals(original, uncroppedPreviewUrl(original, preview))
        }
    }

    @Test
    fun photosKeepTheRequestedPreviewQuality() {
        for (size in listOf("1200x800", "1080x1920", "640x640")) {
            val original = "https://image.coolapk.com/feed/sample@$size.jpg"
            assertEquals("$original.s.jpg", uncroppedPreviewUrl(original, "$original.s.jpg"))
        }
    }

    @Test
    fun encodedDimensionsAndQueryStringsPreserveTheCompleteUrl() {
        val original = "https://image.coolapk.com/feed/sample%40640x6857.jpg?download=1#image"
        assertEquals(original, uncroppedPreviewUrl(original, "thumbnail"))
    }

    @Test
    fun widePanoramasAlsoUseCompleteSource() {
        val original = "https://image.coolapk.com/feed/sample@12000x600.jpg"
        assertEquals(original, uncroppedPreviewUrl(original, "thumbnail"))
    }

    @Test
    fun missingOrInvalidDimensionsKeepTheExistingSource() {
        for (path in listOf("photo.jpg", "photo@0x6857.jpg", "photo@640x999999999999999.jpg")) {
            assertEquals(
                "thumbnail",
                uncroppedPreviewUrl("https://image.coolapk.com/$path", "thumbnail"),
            )
        }
    }
}
