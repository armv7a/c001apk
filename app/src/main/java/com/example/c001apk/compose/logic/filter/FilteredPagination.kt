package com.example.c001apk.compose.logic.filter

/** True means the bounded batch was exhausted. A manual retry starts a new batch. */
internal suspend fun <T> loadFilteredPages(
    maxRequests: Int = 3,
    fetch: suspend () -> T,
    accept: suspend (T, Int) -> Boolean,
): Boolean {
    require(maxRequests > 0)
    repeat(maxRequests) { attempt ->
        if (accept(fetch(), attempt)) return false
    }
    return true
}
