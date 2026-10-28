package com.example.c001apk.compose.logic.filter

import com.example.c001apk.compose.util.readAtMost
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*

/** Public rules use an independent transport without Coolapk account headers or cookies. */
@Singleton
class TwilightRulesClient internal constructor(private val client: OkHttpClient) {
    @Inject constructor() : this(OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build())

    suspend fun fetch(): TwilightRuleSet = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(Request.Builder().url(URL)
            .header("Accept", "application/json").header("User-Agent", "c001apk-content-filter/1").build())
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }
            override fun onResponse(call: Call, response: Response) {
                val result = runCatching { response.use(::readRules) }
                if (continuation.isActive) result.fold(continuation::resume, continuation::resumeWithException)
            }
        })
    }

    companion object {
        const val URL = "https://raw.githubusercontent.com/TG-Twilight/c001apk/filters/rules.json"
        internal fun readRules(response: Response): TwilightRuleSet {
            require(response.code == 200) { "规则下载失败（HTTP ${response.code}）" }
            val body = requireNotNull(response.body) { "规则文件为空" }
            val type = body.contentType()?.let { "${it.type}/${it.subtype}" }
            require(type in setOf("application/json", "text/plain")) { "规则文件格式无效" }
            require(body.contentLength() in -1L..TwilightRuleSet.MAX_BYTES.toLong()) { "规则文件过大" }
            val bytes = body.byteStream().readAtMost(TwilightRuleSet.MAX_BYTES + 1)
            require(bytes.size <= TwilightRuleSet.MAX_BYTES) { "规则文件过大" }
            val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
            return TwilightRuleSet.parse(text)
        }
    }
}
