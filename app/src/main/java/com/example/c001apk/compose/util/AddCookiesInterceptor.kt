package com.example.c001apk.compose.util

import com.example.c001apk.compose.constant.Constants.APP_ID
import com.example.c001apk.compose.constant.Constants.CHANNEL
import com.example.c001apk.compose.constant.Constants.DARK_MODE
import com.example.c001apk.compose.constant.Constants.LOCALE
import com.example.c001apk.compose.constant.Constants.MODE
import com.example.c001apk.compose.constant.Constants.REQUEST_WITH
import com.example.c001apk.compose.logic.network.CaptchaProof
import com.example.c001apk.compose.logic.network.TokenV3
import com.example.c001apk.compose.logic.network.session.AcceptedApiSession
import com.example.c001apk.compose.logic.network.session.ApiSessions
import com.example.c001apk.compose.logic.network.session.RequestSession
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

/** Created by bggRGjQaUbCoE on 2024/6/3 */
object AddCookiesInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.tag(RequestSession::class.java)?.isCurrent() == false)
            throw IOException("账号或设备已变化，请重新加载")
        val context = ApiSessions.store.capture()
        val builder = CoolapkRequestPolicy.withoutPrivateHeaders(request)
        val api = CoolapkRequestPolicy.isApi(request.url)
        if (!api && !CoolapkRequestPolicy.isPublicFeed(request)) {
            return chain.proceed(builder.build())
        }
        val session = CookieUtil.accountSession
        val deviceCode = CookieUtil.xAppDevice
        val token = TokenV3.create(deviceCode, CookieUtil.versionCode.toInt())
        builder.apply {
            header("User-Agent", CookieUtil.userAgent)
            header("X-Requested-With", REQUEST_WITH)
            header("X-Sdk-Int", CookieUtil.sdkInt)
            header("X-Sdk-Locale", LOCALE)
            header("X-App-Id", APP_ID)
            header("X-App-Token", token)
            header("X-App-Version", CookieUtil.versionName)
            header("X-App-Code", CookieUtil.versionCode)
            header("X-Api-Version", CookieUtil.apiVersion)
            header("X-App-Device", deviceCode)
            header("X-Dark-Mode", DARK_MODE)
            header("X-App-Channel", CHANNEL)
            header("X-App-Mode", MODE)
            header("X-App-Supported", CookieUtil.versionCode)
            if (api && session.isLogin)
                header(
                    "Cookie",
                    listOf(
                            "uid=${session.uid}; username=${session.username}; token=${session.token}",
                            ApiSessions.store.cookieFor(request.url),
                        )
                        .filter { it.isNotEmpty() }
                        .joinToString("; "),
                )
            else if (api) header("Cookie", ApiSessions.store.cookieFor(request.url))
        }
        if (api) {
            val validation =
                request.tag(CaptchaProof::class.java)?.encoded ?: ApiSessions.store.validation()
            if (validation != null) {
                val cookie = builder.build().header("Cookie").orEmpty()
                builder.header(
                    "Cookie",
                    listOf(cookie, "validate=$validation")
                        .filter { it.isNotEmpty() }
                        .joinToString("; "),
                )
            }
        }
        val response = chain.proceed(builder.build())
        val accepted =
            if (api)
                ApiSessions.store.save(request.url, response.headers.values("Set-Cookie"), context)
            else null
        return response
            .newBuilder()
            .request(
                response.request
                    .newBuilder()
                    .tag(AcceptedApiSession::class.java, AcceptedApiSession(accepted))
                    .build(),
            )
            .build()
            .also { response ->
                if (
                    com.example.c001apk.compose.BuildConfig.DEBUG &&
                        response.request.url.encodedPath.startsWith("/v6/")
                ) {
                    android.util.Log.d(
                        "CoolApkApi",
                        "${response.request.url.encodedPath}: ${response.code}",
                    )
                }
            }
    }
}
