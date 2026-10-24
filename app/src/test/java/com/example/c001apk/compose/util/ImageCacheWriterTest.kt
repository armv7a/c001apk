package com.example.c001apk.compose.util

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImageCacheWriterTest {
    @get:Rule val directory = TemporaryFolder()

    @Test
    fun overlappingDownloadsNeverPublishTheOtherWritersPartialImage() {
        val cache = directory.root.resolve("same-image.jpg")
        val firstBytes = ByteArray(4096) { 17 }
        val secondBytes = ByteArray(4096) { 42 }
        val first = PausedInput(firstBytes)
        val second = PausedInput(secondBytes)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val firstDownload = executor.submit {
                writeImageCache(cache, first, firstBytes.size.toLong())
            }
            assertTrue(first.partialRead.await(5, TimeUnit.SECONDS))
            val secondDownload = executor.submit {
                writeImageCache(cache, second, secondBytes.size.toLong())
            }
            assertTrue(second.partialRead.await(5, TimeUnit.SECONDS))
            assertFalse(cache.exists())

            first.finish.countDown()
            firstDownload.get(5, TimeUnit.SECONDS)
            assertArrayEquals(firstBytes, cache.readBytes())

            second.finish.countDown()
            secondDownload.get(5, TimeUnit.SECONDS)
            assertArrayEquals(secondBytes, cache.readBytes())
            assertEquals(listOf(cache), directory.root.listFiles()!!.toList())
        } finally {
            first.finish.countDown()
            second.finish.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun interruptedOrTruncatedDownloadsPreserveTheCompleteCachedImage() {
        val cache = directory.newFile("previous.jpg")
        val previous = byteArrayOf(1, 2, 3, 4)
        cache.writeBytes(previous)
        val broken =
            object : InputStream() {
                override fun read(): Int = throw IOException("connection lost")
            }
        val invalidInputs =
            listOf(
                broken to -1L,
                ByteArrayInputStream(byteArrayOf(1, 2)) to 4L,
                ByteArrayInputStream(byteArrayOf()) to -1L,
            )
        invalidInputs.forEach { (input, length) ->
            assertThrows(IOException::class.java) { writeImageCache(cache, input, length) }
            assertArrayEquals(previous, cache.readBytes())
            assertEquals(listOf(cache), directory.root.listFiles()!!.toList())
        }
    }

    @Test
    fun cancellationBeforePublicationLeavesNoCacheEntry() {
        val cache = directory.root.resolve("canceled.jpg")
        var canceled = false
        assertThrows(IOException::class.java) {
            writeImageCache(
                cache,
                byteArrayOf(1, 2, 3).inputStream(),
                3,
                isCanceled = { canceled },
                onProgress = { canceled = true },
            )
        }
        assertFalse(cache.exists())
        assertTrue(directory.root.listFiles()!!.isEmpty())
    }

    @Test
    fun unknownContentLengthStillPublishesTheWholeImage() {
        val cache = directory.root.resolve("chunked.jpg")
        val image = ByteArray(20000) { it.toByte() }
        writeImageCache(cache, image.inputStream(), -1)
        assertArrayEquals(image, cache.readBytes())
    }

    private class PausedInput(private val bytes: ByteArray) : InputStream() {
        val partialRead = CountDownLatch(1)
        val finish = CountDownLatch(1)
        private var position = 0

        override fun read(): Int = error("Use bulk reads")

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (position >= bytes.size) return -1
            if (position > 0) {
                partialRead.countDown()
                check(finish.await(5, TimeUnit.SECONDS)) { "Download was never resumed" }
            }
            val count = minOf(length, bytes.size / 2, bytes.size - position)
            bytes.copyInto(buffer, offset, position, position + count)
            position += count
            return count
        }
    }
}
