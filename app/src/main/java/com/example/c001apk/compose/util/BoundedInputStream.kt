package com.example.c001apk.compose.util

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** InputStream.readNBytes is unavailable on older Android; keep bounded download behavior. */
internal fun InputStream.readAtMost(limit: Int): ByteArray {
    require(limit >= 0)
    val result = ByteArrayOutputStream(minOf(limit, DEFAULT_BUFFER_SIZE))
    val buffer = ByteArray(minOf(limit, DEFAULT_BUFFER_SIZE))
    var remaining = limit
    while (remaining > 0) {
        val count = read(buffer, 0, minOf(buffer.size, remaining))
        if (count < 0) break
        if (count == 0) {
            val single = read()
            if (single < 0) break
            result.write(single)
            remaining--
        } else {
            result.write(buffer, 0, count)
            remaining -= count
        }
    }
    return result.toByteArray()
}
