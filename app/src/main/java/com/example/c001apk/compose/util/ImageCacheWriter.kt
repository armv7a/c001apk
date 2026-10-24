package com.example.c001apk.compose.util

import java.io.File
import java.io.IOException
import java.io.InputStream

/** Publishes a complete download without exposing another request's unfinished file. */
internal fun writeImageCache(
    destination: File,
    input: InputStream,
    expectedLength: Long,
    isCanceled: () -> Boolean = { false },
    onProgress: (Int) -> Unit = {},
): File {
    val directory = checkNotNull(destination.parentFile)
    directory.mkdirs()
    // A view can request the same original both when its transition finishes and when
    // the thumbnail arrives. Each writer must own its file until it closes and publishes it.
    val temporary = File.createTempFile("image-", ".tmp", directory)
    try {
        var received = 0L
        temporary.outputStream().use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                if (isCanceled()) throw IOException("image request canceled")
                val count = input.read(buffer)
                if (count == -1) break
                output.write(buffer, 0, count)
                received += count
                if (expectedLength > 0) {
                    onProgress(((received * 100) / expectedLength).coerceIn(0, 100).toInt())
                }
            }
        }
        if (received == 0L || (expectedLength >= 0 && received != expectedLength)) {
            throw IOException("incomplete image download")
        }
        if (isCanceled()) throw IOException("image request canceled")
        // Same-directory rename leaves readers with either the previous complete image
        // or this complete image, even when two downloads finish at the same time.
        if (!temporary.renameTo(destination)) throw IOException("image cache rename failed")
        return destination
    } finally {
        temporary.delete()
    }
}
