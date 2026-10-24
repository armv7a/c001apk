package com.example.c001apk.compose.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.compose.di.NetworkModule
import com.example.c001apk.compose.logic.network.ApiFailures
import com.example.c001apk.compose.logic.smid.DigitalUnionDebugEntryPoint
import com.example.c001apk.compose.util.CookieUtil
import com.google.gson.JsonParser
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in read-only smoke test. No credentials, device IDs or response content enter the report. */
class ProtocolReadTest {
    @Test
    fun packagedResourcesSupportCookieAndMarkdownParsing() {
        assertNotNull(
            javaClass.getResourceAsStream("/okhttp3/internal/publicsuffix/publicsuffixes.gz")?.use {
                it.read()
            },
        )
        assertNotNull(
            javaClass.getResourceAsStream("/org/commonmark/internal/util/entities.txt")?.use {
                it.read()
            },
        )
        assertTrue(
            com.example.c001apk.compose.logic.markdown.MarkdownContent.parse("# 标题\n\n**正文** &amp;")
                .isMarkdown,
        )
    }

    @Test
    fun nativeProtocolReadsPublicTimeline() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("apiNetwork") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val prefs =
            EntryPointAccessors.fromApplication(context, DigitalUnionDebugEntryPoint::class.java)
                .preferences()
                .data
                .first()
        CookieUtil.publishAccount(prefs)
        CookieUtil.xAppDevice = prefs.xAppDevice
        CookieUtil.szlmId = prefs.szlmId
        CookieUtil.userAgent = prefs.userAgent
        CookieUtil.versionName = prefs.versionName
        CookieUtil.versionCode = prefs.versionCode
        CookieUtil.apiVersion = prefs.apiVersion
        CookieUtil.sdkInt = prefs.sdkInt
        val client =
            NetworkModule.provideOkHttpClient()
                .newBuilder()
                .callTimeout(25, TimeUnit.SECONDS)
                .build()
        try {
            client
                .newCall(
                    Request.Builder()
                        .url("https://api.coolapk.com/v6/main/indexV8?page=1&firstLaunch=0")
                        .build(),
                )
                .execute()
                .use { response ->
                    assertTrue("Timeline HTTP status: ${response.code}", response.isSuccessful)
                    val body = JsonParser.parseString(response.body!!.string()).asJsonObject
                    assertTrue(
                        "Timeline did not contain a data array",
                        body.get("data")?.isJsonArray == true,
                    )
                    assertTrue("Timeline data was empty", body.getAsJsonArray("data").size() > 0)
                }
        } catch (e: Exception) {
            val clue =
                when {
                    e.message.orEmpty().contains("设备已变化") -> "identity changed"
                    e.message.orEmpty().contains("unexpected end", ignoreCase = true) ->
                        "unexpected end of stream"
                    e.message.orEmpty().contains("reset", ignoreCase = true) -> "connection reset"
                    else -> ApiFailures.safeMessage(e)
                }
            throw AssertionError(
                "Read-only API failed: ${e.javaClass.simpleName}; cause=${e.cause?.javaClass?.simpleName}; category=$clue",
            )
        }
    }

    @Test
    fun nativeImagesDecodeAvatarThumbnailAndFullSize() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("imageNetwork") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        reportImageDiagnostic("preferences start")
        val prefs =
            EntryPointAccessors.fromApplication(context, DigitalUnionDebugEntryPoint::class.java)
                .preferences()
                .data
                .first()
        CookieUtil.publishAccount(prefs)
        CookieUtil.xAppDevice = prefs.xAppDevice
        CookieUtil.szlmId = prefs.szlmId
        CookieUtil.userAgent = prefs.userAgent
        CookieUtil.versionName = prefs.versionName
        CookieUtil.versionCode = prefs.versionCode
        CookieUtil.apiVersion = prefs.apiVersion
        CookieUtil.sdkInt = prefs.sdkInt
        reportImageDiagnostic("preferences ready")
        val api =
            NetworkModule.provideOkHttpClient()
                .newBuilder()
                .callTimeout(25, TimeUnit.SECONDS)
                .build()
        reportImageDiagnostic("timeline start")
        val samples = linkedMapOf<String, String>()
        api.newCall(
                Request.Builder()
                    .url("https://api.coolapk.com/v6/main/indexV8?page=1&firstLaunch=0")
                    .tag(
                        com.example.c001apk.compose.logic.network.SkipCaptcha::class.java,
                        com.example.c001apk.compose.logic.network.SkipCaptcha(),
                    )
                    .build(),
            )
            .execute()
            .use { response ->
                assertTrue(response.isSuccessful)
                val rows =
                    JsonParser.parseString(response.body!!.string())
                        .asJsonObject
                        .getAsJsonArray("data")
                for (element in rows) {
                    if (!element.isJsonObject) continue
                    val row = element.asJsonObject
                    row.get("userAvatar")
                        ?.takeIf { it.isJsonPrimitive }
                        ?.asString
                        ?.takeIf { it.startsWith("http") }
                        ?.let { samples.putIfAbsent("avatar", it) }
                    row.get("picArr")
                        ?.takeIf { it.isJsonArray }
                        ?.asJsonArray
                        ?.firstOrNull()
                        ?.takeIf { it.isJsonPrimitive }
                        ?.asString
                        ?.takeIf { it.startsWith("http") }
                        ?.let {
                            samples.putIfAbsent("full", it)
                            samples.putIfAbsent(
                                "thumbnail",
                                it +
                                    com.example.c001apk.compose.constant.Constants.SUFFIX_THUMBNAIL,
                            )
                        }
                }
            }
        reportImageDiagnostic("timeline parsed")
        assertEquals("Expected avatar and feed image samples", 3, samples.size)
        val failures = mutableListOf<String>()
        val raw =
            okhttp3.OkHttpClient.Builder()
                .addNetworkInterceptor(com.example.c001apk.compose.util.AddCookiesInterceptor)
                .callTimeout(20, TimeUnit.SECONDS)
                .build()
        for ((kind, source) in samples) {
            val url = source.replaceFirst("http://", "https://")
            reportImageDiagnostic("$kind request start")
            val result =
                kotlinx.coroutines.withTimeout(25_000) {
                    coil.Coil.imageLoader(context)
                        .execute(
                            coil.request.ImageRequest.Builder(context)
                                .data(url)
                                .addHeader("User-Agent", prefs.userAgent)
                                .allowHardware(false)
                                .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                                .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                                .size(800)
                                .build(),
                        )
                }
            when (result) {
                is coil.request.SuccessResult ->
                    reportImageDiagnostic(
                        "$kind decoded ${result.drawable.intrinsicWidth}x${result.drawable.intrinsicHeight}",
                    )
                is coil.request.ErrorResult -> {
                    val status = runCatching {
                        raw.newCall(
                                Request.Builder()
                                    .url(url)
                                    .header("User-Agent", prefs.userAgent)
                                    .build(),
                            )
                            .execute()
                            .use { "HTTP ${it.code}; type=${it.header("Content-Type")}" }
                    }
                        .getOrElse { it.javaClass.simpleName }
                    val error =
                        "$kind: ${result.throwable.javaClass.simpleName}; cause=${result.throwable.cause?.javaClass?.simpleName}; $status"
                    reportImageDiagnostic(error)
                    failures += error
                }
            }
        }
        assertTrue(failures.joinToString("; "), failures.isEmpty())
    }

    private fun reportImageDiagnostic(message: String) {
        InstrumentationRegistry.getInstrumentation()
            .sendStatus(2, android.os.Bundle().apply { putString("stream", message + "\n") })
    }
}
