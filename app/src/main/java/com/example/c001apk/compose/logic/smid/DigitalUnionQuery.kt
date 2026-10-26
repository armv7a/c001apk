package com.example.c001apk.compose.logic.smid

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

internal const val DIGITAL_UNION_FALLBACK_URL = "https://global-auni.checktrustworthiness.com"

/** Reserve half of the existing 35-second budget for each endpoint. */
internal suspend fun queryDigitalUnionId(
    endpointTimeoutMillis: Long = 17_500,
    retryDelayMillis: Long = 2_500,
    query: suspend (String?) -> String?,
): String? {
    for (url in listOf(null, DIGITAL_UNION_FALLBACK_URL)) {
        val id = withTimeoutOrNull(endpointTimeoutMillis) {
            repeat(4) { attempt ->
                if (attempt > 0) delay(retryDelayMillis)
                val result = try {
                    query(url)?.trim()?.takeIf { it.length >= 16 && it != "null" }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                if (result != null) return@withTimeoutOrNull result
            }
            null
        }
        if (id != null) return id
    }
    return null
}
