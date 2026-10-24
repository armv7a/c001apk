package com.example.c001apk.compose.util

import java.net.URI
import java.net.URLDecoder

/** Normalize before routing; a host merely containing 'coolapk' is never a first-party link. */
data class CoolapkLink(
    val path: String,
    val segments: List<String>,
    val query: Map<String, String>,
) {
    companion object {
        val hosts =
            setOf(
                "coolapk.com",
                "www.coolapk.com",
                "m.coolapk.com",
                "coolapk1s.com",
                "www.coolapk1s.com",
            )

        fun parse(value: String): CoolapkLink? = runCatching {
            if (value.length > 8192 || value.any { it.code < 32 || it == '\\' }) return null
            val raw = value.trim()
            if (raw.startsWith("#/")) return CoolapkLink(raw, emptyList(), emptyMap())
            val uri = URI(raw)
            if (uri.userInfo != null) return null
            val path =
                when (uri.scheme?.lowercase()) {
                    null ->
                        if (raw.startsWith('/') && !raw.startsWith("//")) uri.rawPath
                        else return null
                    "http",
                    "https" ->
                        if (uri.host?.lowercase() in hosts && uri.port in setOf(-1, 80, 443))
                            uri.rawPath
                        else return null
                    "coolmarket" ->
                        if (uri.host?.lowercase() in hosts || uri.host == "com.coolapk.market")
                            uri.rawPath
                        else "/${uri.host.orEmpty()}${uri.rawPath.orEmpty()}"
                    else -> return null
                }
            fun decode(text: String) = URLDecoder.decode(text.replace("+", "%2B"), "UTF-8")
            val segments = path.trim('/').split('/').filter { it.isNotEmpty() }.map(::decode)
            if (
                segments.any {
                    it in setOf(".", "..") || it.any { c -> c.code < 32 || c == '/' || c == '\\' }
                }
            )
                return null
            val query =
                uri.rawQuery
                    .orEmpty()
                    .split('&')
                    .filter { it.isNotEmpty() }
                    .associate {
                        URLDecoder.decode(it.substringBefore('='), "UTF-8") to
                            URLDecoder.decode(it.substringAfter('=', ""), "UTF-8")
                    }
            CoolapkLink(
                path + if (uri.rawQuery != null) "?${uri.rawQuery}" else "",
                segments,
                query,
            )
        }
            .getOrNull()

        fun external(value: String): Boolean = runCatching {
            val uri = URI(value)
            uri.scheme?.lowercase() in setOf("http", "https") &&
                !uri.host.isNullOrEmpty() &&
                uri.userInfo == null &&
                value.none { it.code < 32 || it == '\\' }
        }
            .getOrDefault(false)

        fun fromSharedText(text: String): String? =
            Regex("(?:https?://|coolmarket://)[^\\s<>\"']+")
                .findAll(text.take(32768))
                .map { it.value.trimEnd('。', '，', ')', '）') }
                .firstOrNull { parse(it) != null }
    }
}
