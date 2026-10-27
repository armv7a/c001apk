package com.example.c001apk.compose.util

import java.net.URLDecoder
import java.net.URLEncoder

internal fun encodeCookieValue(value: String?): String = URLEncoder.encode(value.orEmpty(), "UTF-8")

/** Cookie usernames are form-encoded; typed navigation arguments and post text are not. */
internal fun decodeCookieValue(value: String): String =
    try {
        URLDecoder.decode(value, "UTF-8")
    } catch (_: IllegalArgumentException) {
        // A malformed server cookie must not prevent the profile or account list from rendering.
        value
    }
