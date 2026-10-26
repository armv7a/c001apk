package com.example.c001apk.compose.util

import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Assert.*
import org.junit.Test

class BoundedInputStreamTest {
    @Test
    fun enforcesDownloadLimitWithoutReadingTheRestOfTheStream() {
        val input = ByteArrayInputStream(ByteArray(100) { it.toByte() })
        assertArrayEquals(ByteArray(12) { it.toByte() }, input.readAtMost(12))
        assertEquals(88, input.available())
        assertEquals(12, input.read())
    }

    @Test
    fun preservesShortFilesAndZeroLengthRequests() {
        val input = ByteArrayInputStream(byteArrayOf(1, 2, 3))
        assertTrue(input.readAtMost(0).isEmpty())
        assertArrayEquals(byteArrayOf(1, 2, 3), input.readAtMost(100))
        assertTrue(input.readAtMost(100).isEmpty())
    }

    @Test
    fun handlesStreamsThatTemporarilyReturnZeroWithoutSpinningOrLosingData() {
        val input =
            object : InputStream() {
                private val bytes = ByteArrayInputStream(byteArrayOf(4, 5, 6))

                override fun read() = bytes.read()

                override fun read(buffer: ByteArray, offset: Int, length: Int) = 0
            }
        assertArrayEquals(byteArrayOf(4, 5, 6), input.readAtMost(10))
    }
}
