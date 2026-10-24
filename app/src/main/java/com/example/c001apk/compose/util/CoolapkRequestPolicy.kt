package com.example.c001apk.compose.util

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request

/** The same boundary is checked on every redirect hop, before bytes leave the client. */
object CoolapkRequestPolicy {
    fun isApi(url: HttpUrl) =
        url.isHttps && url.port == 443 && url.host in setOf("api.coolapk.com", "api2.coolapk.com")

    fun isAccount(url: HttpUrl) =
        url.isHttps && url.port == 443 && url.host == "account.coolapk.com"

    fun isPublicFeed(request: Request) =
        request.method == "GET" &&
            request.url.isHttps &&
            request.url.port == 443 &&
            request.url.host == "www.coolapk.com" &&
            Regex("^/feed/[0-9]+$").matches(request.url.encodedPath)

    fun withoutPrivateHeaders(request: Request): Request.Builder {
        val builder = request.newBuilder()
        request.headers
            .names()
            .filter {
                val name = it.lowercase()
                name == "cookie" ||
                    name == "authorization" ||
                    name == "user-agent" ||
                    name == "x-requested-with" ||
                    name.startsWith("x-app-") ||
                    name.startsWith("x-sdk-") ||
                    name == "x-api-version" ||
                    name == "x-dark-mode"
            }
            .forEach(builder::removeHeader)
        return builder
    }

    val accountBoundary = Interceptor { chain ->
        val request = chain.request()
        chain.proceed(
            if (isAccount(request.url)) request else withoutPrivateHeaders(request).build(),
        )
    }
}
