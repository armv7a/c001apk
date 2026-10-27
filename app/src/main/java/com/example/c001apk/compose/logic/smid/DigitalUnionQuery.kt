package com.example.c001apk.compose.logic.smid

import cn.shuzilm.core.DigitalUnionEndpoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

internal const val DIGITAL_UNION_FALLBACK_URL = "https://global-auni.checktrustworthiness.com"
internal val DIGITAL_UNION_FALLBACK_ENDPOINT =
    checkNotNull(DigitalUnionEndpoint.toNativeAuthority(DIGITAL_UNION_FALLBACK_URL))
internal const val DIGITAL_UNION_ENDPOINT_TIMEOUT_MILLIS = 17_500L

/** The transport bounds each request and finishes teardown before returning or throwing. */
internal suspend fun queryDigitalUnionId(
    query: suspend (String?) -> String?,
): String? {
    for (endpoint in listOf(null, DIGITAL_UNION_FALLBACK_ENDPOINT)) {
        val id = try {
            validDigitalUnionId(query(endpoint))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        if (id != null) return id
    }
    return null
}

/** Poll within one worker so asynchronous native registration has time to finish. */
internal suspend fun pollDigitalUnionId(
    retryDelayMillis: Long = 2_500,
    query: suspend () -> String?,
): String? {
    repeat(4) { attempt ->
        if (attempt > 0) delay(retryDelayMillis)
        val id = try {
            validDigitalUnionId(query())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        if (id != null) return id
    }
    return null
}

internal fun validDigitalUnionId(value: String?): String? =
    value?.trim()?.takeIf { it.length >= 16 && it != "null" }
